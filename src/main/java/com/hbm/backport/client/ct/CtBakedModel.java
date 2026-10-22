// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.ct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-105 block model with 1.7.10 connected textures, see {@link ConnectedTextures}.
 *
 * <p>The model data holds which of the 26 surrounding blocks the block connects to. A face quad that uses a listed
 * texture is cut into four quarters; each quarter looks at its side neighbour, its corner neighbour and its
 * top/bottom neighbour in the plane of the face (1.7.10 CTContext.cornerType) and takes:
 * <ul>
 * <li>nothing connected: the same quarter of the plain texture;</li>
 * <li>otherwise a quarter of the 4x4 {@code _ct} sheet: connected all round (top-left 2x2), vertical only
 * (top-right), horizontal only (bottom-left), both sides but not the corner (bottom-right) — 1.7.10 IconCT.</li>
 * </ul>
 * Left/right/up/down are taken from the quad's own texture coordinates, so the result matches the texture as the
 * model shows it on every face. Quads that don't cover the whole texture are left as they are.
 */
final class CtBakedModel extends BakedModelWrapper<BakedModel> {

    static final ModelProperty<Integer> NEIGHBOURS = new ModelProperty<>();

    private static final int STRIDE = 8; // DefaultVertexFormat.BLOCK in ints
    private static final int POS = 0;
    private static final int UV = 4;

    // quarter kinds, 1.7.10 CT.f/c/j/h/v
    private static final int F = 0, C = 1, J = 2, H = 3, V = 4;
    // top-left cell of each kind on the 4x4 _ct sheet (u, v)
    private static final int[] SHEET_U = {0, 0, 2, 0, 2};
    private static final int[] SHEET_V = {0, 0, 2, 2, 0};

    private final Map<ResourceLocation, TextureAtlasSprite> sheets;
    private final Set<Block> connect;

    CtBakedModel(BakedModel original, Map<ResourceLocation, TextureAtlasSprite> sheets, Set<Block> connect) {
        super(original);
        this.sheets = sheets;
        this.connect = connect;
    }

    private static int bit(int dx, int dy, int dz) {
        return 1 << ((dx + 1) * 9 + (dy + 1) * 3 + (dz + 1));
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        data = super.getModelData(level, pos, state, data);
        int mask = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (connect.contains(level.getBlockState(p.setWithOffset(pos, dx, dy, dz)).getBlock()))
                        mask |= bit(dx, dy, dz);
                }
            }
        }
        return data.derive().with(NEIGHBOURS, mask).build();
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData data,
            @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        Integer mask = data.get(NEIGHBOURS);
        if (mask == null || quads.isEmpty()) return quads;
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            TextureAtlasSprite sheet = sheets.get(quad.getSprite().contents().name());
            if (sheet == null) {
                if (out != null) out.add(quad);
                continue;
            }
            if (out == null) out = new ArrayList<>(quads.subList(0, i));
            if (!split(quad, sheet, mask, out)) out.add(quad);
        }
        return out == null ? quads : out;
    }

    /** Cuts a quad that covers its whole texture into four quarters; false if the quad can't be cut. */
    private static boolean split(BakedQuad quad, TextureAtlasSprite sheet, int mask, List<BakedQuad> out) {
        int[] v = quad.getVertices();
        if (v.length < STRIDE * 4) return false;
        TextureAtlasSprite plain = quad.getSprite();
        float du = plain.getU1() - plain.getU0();
        float dv = plain.getV1() - plain.getV0();
        if (du == 0 || dv == 0) return false;

        // texture corner of every vertex: s (0 left, 1 right), t (0 top, 1 bottom)
        int[] cs = new int[4];
        int[] ct = new int[4];
        int[] vertexAt = {-1, -1, -1, -1}; // index s + 2t -> vertex
        float[][] p = new float[4][3];
        for (int k = 0; k < 4; k++) {
            int o = k * STRIDE;
            for (int a = 0; a < 3; a++) p[k][a] = Float.intBitsToFloat(v[o + POS + a]);
            float s = (Float.intBitsToFloat(v[o + UV]) - plain.getU0()) / du;
            float t = (Float.intBitsToFloat(v[o + UV + 1]) - plain.getV0()) / dv;
            cs[k] = edge(s);
            ct[k] = edge(t);
            if (cs[k] < 0 || ct[k] < 0) return false;
            int at = cs[k] + 2 * ct[k];
            if (vertexAt[at] != -1) return false;
            vertexAt[at] = k;
        }
        float[] p00 = p[vertexAt[0]], p10 = p[vertexAt[1]], p01 = p[vertexAt[2]], p11 = p[vertexAt[3]];
        Direction right = dominant(p10[0] - p00[0], p10[1] - p00[1], p10[2] - p00[2]);
        Direction down = dominant(p01[0] - p00[0], p01[1] - p00[1], p01[2] - p00[2]);
        if (right == null || down == null || right.getAxis() == down.getAxis()) return false;

        // 1.7.10 CTContext: 0 1 2 / 3 _ 4 / 5 6 7 around the face, in texture orientation
        boolean up = con(mask, 0, -1, right, down), left = con(mask, -1, 0, right, down);
        boolean rt = con(mask, 1, 0, right, down), dn = con(mask, 0, 1, right, down);
        int[] kind = {
            kind(left, con(mask, -1, -1, right, down), up),
            kind(rt, con(mask, 1, -1, right, down), up),
            kind(left, con(mask, -1, 1, right, down), dn),
            kind(rt, con(mask, 1, 1, right, down), dn)
        };

        for (int q = 0; q < 4; q++) {
            int qs = q & 1;
            int qt = q >> 1;
            TextureAtlasSprite sprite;
            float u0, v0, size;
            if (kind[q] == F) {
                sprite = plain;
                u0 = qs * 0.5F;
                v0 = qt * 0.5F;
                size = 0.5F;
            } else {
                sprite = sheet;
                u0 = (SHEET_U[kind[q]] + qs) * 0.25F;
                v0 = (SHEET_V[kind[q]] + qt) * 0.25F;
                size = 0.25F;
            }
            int[] nv = v.clone();
            for (int k = 0; k < 4; k++) {
                int o = k * STRIDE;
                float s = (qs + cs[k]) * 0.5F;
                float t = (qt + ct[k]) * 0.5F;
                for (int a = 0; a < 3; a++) {
                    float pos = (1 - s) * (1 - t) * p00[a] + s * (1 - t) * p10[a] + (1 - s) * t * p01[a] + s * t * p11[a];
                    nv[o + POS + a] = Float.floatToRawIntBits(pos);
                }
                float uu = sprite.getU0() + (u0 + cs[k] * size) * (sprite.getU1() - sprite.getU0());
                float vv = sprite.getV0() + (v0 + ct[k] * size) * (sprite.getV1() - sprite.getV0());
                nv[o + UV] = Float.floatToRawIntBits(uu);
                nv[o + UV + 1] = Float.floatToRawIntBits(vv);
            }
            out.add(new BakedQuad(nv, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return true;
    }

    private static int edge(float f) {
        if (Math.abs(f) < 0.01F) return 0;
        if (Math.abs(f - 1) < 0.01F) return 1;
        return -1;
    }

    private static @Nullable Direction dominant(float x, float y, float z) {
        float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ax < 1e-4F && ay < 1e-4F && az < 1e-4F) return null;
        if (ax >= ay && ax >= az) return x > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= az) return y > 0 ? Direction.UP : Direction.DOWN;
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Neighbour at a steps right and b steps down in the plane of the face. */
    private static boolean con(int mask, int a, int b, Direction right, Direction down) {
        int dx = a * right.getStepX() + b * down.getStepX();
        int dy = a * right.getStepY() + b * down.getStepY();
        int dz = a * right.getStepZ() + b * down.getStepZ();
        return (mask & bit(dx, dy, dz)) != 0;
    }

    /** 1.7.10 CTContext.cornerType. */
    private static int kind(boolean hor, boolean corner, boolean vert) {
        if (vert && hor && corner) return C;
        if (vert && hor) return J;
        if (vert) return V;
        if (hor) return H;
        return F;
    }
}
