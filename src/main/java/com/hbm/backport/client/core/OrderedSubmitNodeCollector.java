// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.blockmodel.BakedQuad;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.OrderedSubmitNodeCollector}: the submit side of the 26.x
 * extract/submit renderer split. 1.21.1 renders immediately, so the collector the bridge hands to
 * {@code submit} ({@link ImmediateSubmitNodeCollector}) draws each node into the frame's
 * {@code MultiBufferSource} as it arrives.
 *
 * <p>Everything that can be expressed as custom geometry defaults to {@link
 * #submitCustomGeometry}, so decorating collectors (crumbling, mirroring) only need to wrap that one.
 * Nodes that 1.21.1 draws natively elsewhere (entity shadows, fire, leashes) are no-ops by default.
 *
 * <p>Not bridged (types owned by other topics / unused outside the tree's own collectors):
 * submitBlockModel, submitBreakingBlockModel, submitQuadParticleGroup,
 * submitGizmoPrimitives.
 */
public interface OrderedSubmitNodeCollector {

    void submitCustomGeometry(
            PoseStack poseStack,
            RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer);

    void submitText(
            PoseStack poseStack,
            float x,
            float y,
            FormattedCharSequence string,
            boolean dropShadow,
            Font.DisplayMode displayMode,
            int lightCoords,
            int color,
            int backgroundColor,
            int outlineColor);

    void submitNameTag(
            PoseStack poseStack,
            @Nullable Vec3 nameTagAttachment,
            int offset,
            Component name,
            boolean seeThrough,
            int lightCoords,
            CameraRenderState camera);

    /** 26.x item layer: baked quads (26.x-shaped, carrying their render type) with tint colors. */
    void submitItem(
            PoseStack poseStack,
            ItemDisplayContext displayContext,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            int[] tintLayers,
            List<BakedQuad> quads,
            ItemStackRenderState.FoilType foilType);

    // ---- nodes 1.21.1 draws natively ------------------------------------------------------

    /** backport: 1.21.1's EntityRenderDispatcher draws shadows itself (EntityRenderer.shadowRadius). */
    default void submitShadow(
            PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {}

    /** backport: 1.21.1's EntityRenderDispatcher draws entity fire itself. */
    default void submitFlame(
            PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {}

    /** backport: 1.21.1's MobRenderer draws leashes itself. */
    default void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {}

    // ---- models ---------------------------------------------------------------------------

    default <S> void submitModel(
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            @Nullable TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        // backport: outline (glowing) color and crumbling are handled by 1.21.1 itself (outline
        // buffer / crumbling-wrapped MultiBufferSource of the calling renderer)
        submitCustomGeometry(
                poseStack,
                renderType,
                (pose, buffer) -> {
                    model.setupAnim(state);
                    VertexConsumer vc = sprite != null ? sprite.wrap(buffer) : buffer;
                    model.renderToBuffer(
                            SubmitNodeCollector.poseStackOf(pose),
                            vc,
                            lightCoords,
                            overlayCoords,
                            tintedColor);
                });
    }

    default <S> void submitModel(
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        submitModel(
                model,
                state,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                -1,
                null,
                outlineColor,
                crumblingOverlay);
    }

    /** Texture form: the model's own render type for {@code texture}. */
    default <S> void submitModel(
            Model<? super S> model,
            S state,
            PoseStack poseStack,
            net.minecraft.resources.ResourceLocation texture,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        submitModel(
                model,
                state,
                poseStack,
                model.renderType(texture),
                lightCoords,
                overlayCoords,
                outlineColor,
                crumblingOverlay);
    }

    default void submitModelPart(
            ModelPart part,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            @Nullable TextureAtlasSprite sprite,
            int tintedColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
            int outlineColor) {
        submitCustomGeometry(
                poseStack,
                renderType,
                (pose, buffer) -> {
                    VertexConsumer vc = sprite != null ? sprite.wrap(buffer) : buffer;
                    part.render(
                            SubmitNodeCollector.poseStackOf(pose),
                            vc,
                            lightCoords,
                            overlayCoords,
                            tintedColor);
                });
    }

    default void submitModelPart(
            ModelPart part,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            @Nullable TextureAtlasSprite sprite,
            int tintedColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        submitModelPart(
                part,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                sprite,
                tintedColor,
                crumblingOverlay,
                0);
    }

    default void submitModelPart(
            ModelPart part,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            @Nullable TextureAtlasSprite sprite) {
        submitModelPart(
                part, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, 0);
    }

    default void submitModelPart(
            ModelPart part,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords) {
        submitModelPart(part, poseStack, renderType, lightCoords, overlayCoords, null);
    }

    // ---- blocks ---------------------------------------------------------------------------

    /**
     * 26.x moving block (falling blocks): the block's 1.21.1 baked model, one custom-geometry node per
     * chunk render type (as 1.21.1 FallingBlockRenderer draws it).
     * backport: the outline color is applied by 1.21.1's outline buffer, not per node.
     */
    default void submitMovingBlock(
            PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        net.minecraft.world.level.block.state.BlockState state = movingBlockRenderState.blockState;
        if (state.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) return;
        net.minecraft.client.renderer.block.BlockRenderDispatcher dispatcher =
                net.minecraft.client.Minecraft.getInstance().getBlockRenderer();
        net.minecraft.client.resources.model.BakedModel model = dispatcher.getBlockModel(state);
        long seed = state.getSeed(movingBlockRenderState.randomSeedPos);
        for (RenderType chunkType :
                model.getRenderTypes(
                        state,
                        net.minecraft.util.RandomSource.create(seed),
                        net.neoforged.neoforge.client.model.data.ModelData.EMPTY)) {
            submitCustomGeometry(
                    poseStack,
                    net.neoforged.neoforge.client.RenderTypeHelper.getMovingBlockRenderType(chunkType),
                    (pose, buffer) ->
                            dispatcher
                                    .getModelRenderer()
                                    .tesselateBlock(
                                            movingBlockRenderState,
                                            model,
                                            state,
                                            movingBlockRenderState.blockPos,
                                            SubmitNodeCollector.poseStackOf(pose),
                                            buffer,
                                            false,
                                            net.minecraft.util.RandomSource.create(),
                                            seed,
                                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                                            net.neoforged.neoforge.client.model.data.ModelData.EMPTY,
                                            chunkType));
        }
    }

    // ---- outlines -------------------------------------------------------------------------

    /**
     * 26.x shape outline. backport: line width and the after-terrain pass do not exist per node in
     * 1.21.1 (the lines render type fixes the width); the edges go into {@code renderType} now.
     */
    default void submitShapeOutline(
            PoseStack poseStack,
            VoxelShape shape,
            RenderType renderType,
            int color,
            float width,
            boolean afterTerrain) {
        submitCustomGeometry(
                poseStack,
                renderType,
                (pose, buffer) ->
                        shape.forAllEdges(
                                (x0, y0, z0, x1, y1, z1) -> {
                                    float dx = (float) (x1 - x0);
                                    float dy = (float) (y1 - y0);
                                    float dz = (float) (z1 - z0);
                                    float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                                    if (len > 0F) {
                                        dx /= len;
                                        dy /= len;
                                        dz /= len;
                                    }
                                    buffer.addVertex(pose, (float) x0, (float) y0, (float) z0)
                                            .setColor(color)
                                            .setNormal(pose, dx, dy, dz);
                                    buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                                            .setColor(color)
                                            .setNormal(pose, dx, dy, dz);
                                }));
    }

    // ---- text convenience overloads -------------------------------------------------------

    default void submitText(
            PoseStack poseStack,
            float x,
            float y,
            FormattedCharSequence string,
            boolean dropShadow,
            Font.DisplayMode displayMode,
            int lightCoords,
            int color,
            int backgroundColor) {
        submitText(
                poseStack,
                x,
                y,
                string,
                dropShadow,
                displayMode,
                lightCoords,
                color,
                backgroundColor,
                0);
    }
}
