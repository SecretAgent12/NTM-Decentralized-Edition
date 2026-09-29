// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jspecify.annotations.Nullable;

/**
 * 26.x net.minecraft.client.renderer.block.ModelBlockRenderer(ambientOcclusion, cull, colors):
 * lights each quad of a block model and hands it with its {@link QuadInstance} to a
 * {@link BlockQuadOutput}.
 *
 * <p>The lighting is 1.21.1's own: each (part, face) group of quads is tesselated by the vanilla
 * ModelBlockRenderer (AO or flat, tint, directional shade, baked colours/emission) into a
 * capturing consumer, and the captured per-vertex colour and light become the instance. Order:
 * per part, unculled quads first, then the faces DOWN..EAST (the order the HBM section meshes
 * enumerate quads in).
 */
public final class ModelBlockRenderer {
    private static final Direction[] FACES = Direction.values();

    private final boolean ambientOcclusion;
    private final boolean cull;
    private final net.minecraft.client.renderer.block.ModelBlockRenderer vanilla;

    public ModelBlockRenderer(boolean ambientOcclusion, boolean cull, BlockColors colors) {
        this.ambientOcclusion = ambientOcclusion;
        this.cull = cull;
        this.vanilla = new net.minecraft.client.renderer.block.ModelBlockRenderer(colors);
    }

    public static void clearCache() {
        net.minecraft.client.renderer.block.ModelBlockRenderer.clearCache();
    }

    public static void enableCaching() {
        net.minecraft.client.renderer.block.ModelBlockRenderer.enableCaching();
    }

    /** 26.x: leaves draw opaque unless cutout leaves are enabled. */
    public static boolean forceOpaque(boolean cutoutLeaves, BlockState state) {
        return !cutoutLeaves && state.getBlock() instanceof LeavesBlock;
    }

    public void tesselateBlock(
            BlockQuadOutput output,
            float x,
            float y,
            float z,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            BlockStateModel model,
            long seed) {
        List<BlockStateModelPart> parts = new ArrayList<>(2);
        model.collectParts(level, pos, state, RandomSource.create(seed), parts);
        Capture capture = new Capture();
        PoseStack poseStack = new PoseStack();
        RandomSource random = RandomSource.create();
        boolean ao = ambientOcclusion
                && net.minecraft.client.Minecraft.useAmbientOcclusion()
                && state.getLightEmission(level, pos) == 0;
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (BlockStateModelPart part : parts) {
            boolean partAo = ao && part.useAmbientOcclusion();
            for (int f = -1; f < FACES.length; f++) {
                Direction face = f < 0 ? null : FACES[f];
                List<BakedQuad> quads = part.getQuads(face);
                if (quads.isEmpty()) continue;
                if (face != null && cull) {
                    neighbour.setWithOffset(pos, face);
                    if (!Block.shouldRenderFace(state, level, pos, face, neighbour)) continue;
                }
                List<net.minecraft.client.renderer.block.model.BakedQuad> vanillaQuads = new ArrayList<>(quads.size());
                for (BakedQuad quad : quads) vanillaQuads.add(quad.toVanilla());
                GroupModel group = new GroupModel(face, vanillaQuads, part.particleMaterial().sprite());
                capture.reset(quads.size());
                if (partAo) {
                    vanilla.tesselateWithAO(level, group, state, pos, poseStack, capture, false, random, seed,
                            OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                } else {
                    vanilla.tesselateWithoutAO(level, group, state, pos, poseStack, capture, false, random, seed,
                            OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                }
                for (int i = 0; i < quads.size(); i++) {
                    QuadInstance instance = new QuadInstance();
                    instance.bakedColorsApplied = true;
                    for (int v = 0; v < 4; v++) {
                        int at = i * 4 + v;
                        instance.setColor(v, at < capture.count ? capture.colors[at] : -1);
                        instance.setLightCoords(v, at < capture.count ? capture.light[at] : 0);
                    }
                    output.put(x, y, z, quads.get(i), instance);
                }
            }
        }
    }

    /** One face group as a 1.21.1 model (quads only for its own face). */
    private record GroupModel(
            @Nullable Direction face,
            List<net.minecraft.client.renderer.block.model.BakedQuad> quads,
            TextureAtlasSprite particle)
            implements IDynamicBakedModel {
        @Override
        public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(
                @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                net.minecraft.client.renderer.@Nullable RenderType renderType) {
            return side == face ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean usesBlockLight() {
            return true;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return particle;
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }

    /** Records colour and light of every vertex the vanilla renderer emits. */
    private static final class Capture implements VertexConsumer {
        int[] colors = new int[64];
        int[] light = new int[64];
        int count;

        void reset(int quads) {
            count = 0;
            if (colors.length < quads * 4) {
                colors = new int[quads * 4];
                light = new int[quads * 4];
            }
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int packedLight,
                float nx, float ny, float nz) {
            if (count < colors.length) {
                colors[count] = color;
                light[count] = packedLight;
            }
            count++;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            count++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}
