// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityBlockSpider;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.hbm.backport.client.core.EntityRenderState;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class RenderBlockSpider extends EntityRenderer<EntityBlockSpider, RenderBlockSpider.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.blockspider_tex);
    private static final int[] ODD =
            ResourceManager.blockspider.partIds("Leg1", "Leg3", "Leg5", "Leg7");
    private static final int[] EVEN =
            ResourceManager.blockspider.partIds("Leg2", "Leg4", "Leg6", "Leg8");

    public RenderBlockSpider(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
    }

    private static void legPart(
            PoseStack poseStack, SubmitNodeCollector collector, int light, int part) {
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) ->
                        ResourceManager.blockspider.renderPart(pose, buffer, light, -1, part));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityBlockSpider entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.swing =
                -(Mth.cos(entity.walkAnimation.position(partialTicks) * 0.6662F * 2F) * 0.4F)
                        * entity.walkAnimation.speed(partialTicks)
                        * 57.3F;
        state.disguise = entity.getDisguise();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {

        if (state.disguise == null || state.disguise.isAir()) {
            super.submit(state, poseStack, collector, camera);
            return;
        }

        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - state.bodyYaw));

        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        poseStack.pushPose();
        poseStack.translate(0D, state.swing * 0.005D, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.swing));
        for (int leg : ODD) legPart(poseStack, collector, light, leg);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0D, state.swing * -0.005D, 0D);
        poseStack.mulPose(Axis.YN.rotationDegrees(state.swing));
        for (int leg : EVEN) legPart(poseStack, collector, light, leg);
        poseStack.popPose();

        poseStack.pushPose();

        poseStack.translate(-0.5D, 0.25D, -0.5D);
        com.hbm.backport.client.core.SingleBlockSubmit.submit(
                collector, poseStack, state.disguise, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        // backport: 26.x resolves a BlockModelRenderState; 1.21.1 draws the block state directly
        public net.minecraft.world.level.block.state.@org.jspecify.annotations.Nullable BlockState disguise;
        float bodyYaw;
        float swing;
    }
}
