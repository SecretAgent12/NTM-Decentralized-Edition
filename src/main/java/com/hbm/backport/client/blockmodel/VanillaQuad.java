// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.hbm.backport.client.rendertype.ChunkSectionLayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The 1.21.1 BakedQuad a 26.x {@link BakedQuad} is drawn as. Keeps its source so 1.21.1-side
 * code (lighting mixins, compat) can read the 26.x material info, e.g. the HBM light origin.
 */
public final class VanillaQuad extends net.minecraft.client.renderer.block.model.BakedQuad {

    /** DefaultVertexFormat.BLOCK: pos(3) colour(1) uv0(2) uv2(1) normal(1). */
    public static final int STRIDE = 8;

    private final BakedQuad source;

    private VanillaQuad(int[] vertices, BakedQuad source) {
        super(vertices, source.materialInfo().tintIndex(), source.direction(), source.materialInfo().sprite(),
                source.materialInfo().shade(), source.materialInfo().ambientOcclusion());
        this.source = source;
    }

    public BakedQuad source() {
        return source;
    }

    public int lightOrigin() {
        return source.materialInfo().hbm$lightOrigin();
    }

    static VanillaQuad of(BakedQuad quad) {
        int[] v = new int[STRIDE * 4];
        int emission = quad.materialInfo().lightEmission();
        int light = emission > 0 ? LightTexture.pack(Math.min(emission, 15), 0) : 0;
        int faceNormal = 0;
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            Vector3fc p = quad.position(i);
            long uv = quad.packedUV(i);
            v[o] = Float.floatToRawIntBits(p.x());
            v[o + 1] = Float.floatToRawIntBits(p.y());
            v[o + 2] = Float.floatToRawIntBits(p.z());
            v[o + 3] = argbToAbgr(quad.bakedColors().color(i));
            v[o + 4] = Float.floatToRawIntBits(Uv.unpackU(uv));
            v[o + 5] = Float.floatToRawIntBits(Uv.unpackV(uv));
            v[o + 6] = light;
            int n = quad.bakedNormals().normal(i);
            if (BakedNormals.isUnspecified(n)) {
                if (faceNormal == 0) {
                    faceNormal = BakedNormals.computeQuadNormal(
                            quad.position0(), quad.position1(), quad.position2(), quad.position3());
                }
                n = faceNormal;
            }
            v[o + 7] = n;
        }
        return new VanillaQuad(v, quad);
    }

    static BakedQuad decode(net.minecraft.client.renderer.block.model.BakedQuad quad, ChunkSectionLayer layer, RenderType itemType) {
        int[] v = quad.getVertices();
        Vector3f[] pos = new Vector3f[4];
        long[] uv = new long[4];
        int[] colors = new int[4];
        int[] normals = new int[4];
        int emission = 0;
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            pos[i] = new Vector3f(Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2]));
            colors[i] = abgrToArgb(v[o + 3]);
            uv[i] = Uv.pack(Float.intBitsToFloat(v[o + 4]), Float.intBitsToFloat(v[o + 5]));
            emission = Math.max(emission, LightTexture.block(v[o + 6]));
            normals[i] = v[o + 7] & 0xFFFFFF;
        }
        BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(quad.getSprite(), layer, itemType,
                quad.getTintIndex(), quad.isShade(), emission, quad.hasAmbientOcclusion());
        Direction dir = quad.getDirection();
        return new BakedQuad(pos[0], pos[1], pos[2], pos[3], uv[0], uv[1], uv[2], uv[3], dir, info,
                BakedNormals.of(normals[0], normals[1], normals[2], normals[3]),
                BakedColors.of(colors[0], colors[1], colors[2], colors[3]));
    }

    static int argbToAbgr(int argb) {
        return argb & 0xFF00FF00 | (argb & 0xFF) << 16 | (argb >>> 16) & 0xFF;
    }

    static int abgrToArgb(int abgr) {
        return argbToAbgr(abgr);
    }
}
