// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.hbm.backport.client.core.EntityRenderState;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.ARGB;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

public class RenderCloudFleija extends EntityRenderer<EntityCloudFleija, RenderCloudFleija.State>
        implements ConcurrentRenderStateExtraction {

    private static final int CORE = ARGB.colorFromFloat(1F, 0F, 1F, 1F);
    private static final int SHELL = ARGB.colorFromFloat(1F, 0F, 0.125F, 0.125F);

    public RenderCloudFleija(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    static void submitSphere(
            PoseStack poseStack, SubmitNodeCollector collector, RenderType type, int color) {
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) ->
                        ResourceManager.sphere_new.render(
                                pose, buffer, LightTexture.FULL_BRIGHT, color));
    }

    @Override
    protected boolean affectedByCulling(EntityCloudFleija entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityCloudFleija entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.age + partialTicks;
        state.maxAge = entity.getMaxAge();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        double baseScale = state.age * 2;
        double ageScale = baseScale / state.maxAge;

        poseStack.pushPose();
        {
            double scale = ageScale * 1.2;
            if (scale > 1) scale = Math.max(1 - (scale - 1) * 5, 0);
            scale *= 2 * baseScale;
            poseStack.scale((float) scale, (float) scale, (float) scale);

            submitSphere(poseStack, collector, CloudRenderTypes.OPAQUE, CORE);

            double outerScale = 1.05;
            for (int i = 0; i < 3; i++) {
                poseStack.scale((float) outerScale, (float) outerScale, (float) outerScale);
                submitSphere(poseStack, collector, CloudRenderTypes.ADDITIVE, SHELL);
            }
        }
        poseStack.popPose();

        poseStack.pushPose();
        {
            double shockwave = 5 * baseScale;
            poseStack.scale((float) shockwave, (float) shockwave, (float) shockwave);

            float shockTint = Mth.clamp((1F - (float) ageScale) * 0.75F, 0F, 1F);
            submitSphere(
                    poseStack,
                    collector,
                    CloudRenderTypes.ADDITIVE,
                    ARGB.colorFromFloat(1F, shockTint, shockTint, shockTint));
        }
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float age;
        int maxAge;
    }
}
