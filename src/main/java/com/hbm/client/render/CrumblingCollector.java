// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import java.util.List;
import net.minecraft.client.gui.Font;
import com.hbm.backport.client.core.Model;
import com.hbm.backport.client.core.OrderedSubmitNodeCollector;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.MovingBlockRenderState;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import com.hbm.backport.client.blockmodel.BakedQuad;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

// backport: in 1.21.1 the block entity dispatcher already wraps the MultiBufferSource with the
// crumbling decal for the block entity being mined, so the bridge passes no CrumblingOverlay and
// wrap(...) is a no-op there; this collector only applies when a caller supplies an overlay itself.
// The 26.x block-model / particle / gizmo submissions are not part of the bridge.
public final class CrumblingCollector implements SubmitNodeCollector {

    // backport: 1.21.1 has a ready-made crumbling render type per destroy stage texture
    private static final List<RenderType> STAGES =
            ModelBakery.BREAKING_LOCATIONS.stream().map(RenderType::crumbling).toList();

    private final OrderedSubmitNodeCollector delegate;
    private final @Nullable SubmitNodeCollector orderable;
    private final ModelFeatureRenderer.CrumblingOverlay overlay;

    public static SubmitNodeCollector wrap(
            SubmitNodeCollector collector, BlockEntityRenderState state) {
        ModelFeatureRenderer.CrumblingOverlay overlay = state.breakProgress;
        if (overlay == null || !MultiblockCrumbling.isOwn(state.blockEntityType)) return collector;
        return new CrumblingCollector(collector, collector, overlay);
    }

    private CrumblingCollector(
            OrderedSubmitNodeCollector delegate,
            @Nullable SubmitNodeCollector orderable,
            ModelFeatureRenderer.CrumblingOverlay overlay) {
        this.delegate = delegate;
        this.orderable = orderable;
        this.overlay = overlay;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        if (orderable == null)
            throw new IllegalStateException("an ordered collector cannot be re-ordered");
        return new CrumblingCollector(orderable.order(order), null, overlay);
    }

    @Override
    public void submitCustomGeometry(
            PoseStack poseStack,
            RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        delegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
        if (!renderType.affectsCrumbling()) return;
        PoseStack.Pose decal = overlay.cameraPose();
        delegate.submitCustomGeometry(
                poseStack,
                STAGES.get(overlay.progress()),
                (pose, buffer) ->
                        customGeometryRenderer.render(
                                pose, new SheetedDecalTextureGenerator(buffer, decal, 1.0F)));
    }

    @Override
    public <S> void submitModel(
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
        delegate.submitModel(
                model,
                state,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                tintedColor,
                sprite,
                outlineColor,
                crumblingOverlay != null ? crumblingOverlay : overlay);
    }

    @Override
    public void submitShadow(
            PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadow(poseStack, radius, pieces);
    }

    @Override
    public void submitNameTag(
            PoseStack poseStack,
            @Nullable Vec3 nameTagAttachment,
            int offset,
            Component name,
            boolean seeThrough,
            int lightCoords,
            CameraRenderState camera) {
        delegate.submitNameTag(
                poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, camera);
    }

    @Override
    public void submitText(
            PoseStack poseStack,
            float x,
            float y,
            FormattedCharSequence string,
            boolean dropShadow,
            Font.DisplayMode displayMode,
            int lightCoords,
            int color,
            int backgroundColor,
            int outlineColor) {
        delegate.submitText(
                poseStack,
                x,
                y,
                string,
                dropShadow,
                displayMode,
                lightCoords,
                color,
                backgroundColor,
                outlineColor);
    }

    @Override
    public void submitFlame(
            PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        delegate.submitFlame(poseStack, renderState, rotation);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        delegate.submitLeash(poseStack, leashState);
    }

    @Override
    public void submitMovingBlock(
            PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        delegate.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
    }

    @Override
    public void submitShapeOutline(
            PoseStack poseStack,
            VoxelShape shape,
            RenderType renderType,
            int color,
            float width,
            boolean afterTerrain) {
        delegate.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
    }

    @Override
    public void submitItem(
            PoseStack poseStack,
            ItemDisplayContext displayContext,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            int[] tintLayers,
            List<BakedQuad> quads,
            ItemStackRenderState.FoilType foilType) {
        delegate.submitItem(
                poseStack,
                displayContext,
                lightCoords,
                overlayCoords,
                outlineColor,
                tintLayers,
                quads,
                foilType);
    }
}
