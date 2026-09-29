// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityCloudFleijaRainbow;
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
import net.minecraft.util.RandomSource;

public class RenderCloudRainbow
        extends EntityRenderer<EntityCloudFleijaRainbow, RenderCloudRainbow.State>
        implements ConcurrentRenderStateExtraction {

    private final RandomSource random = RandomSource.create();

    public RenderCloudRainbow(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityCloudFleijaRainbow entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            EntityCloudFleijaRainbow entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.age;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(state.age, state.age, state.age);

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        submitSphere(poseStack, collector, CloudRenderTypes.OPAQUE);
        poseStack.popPose();

        for (float i = 0.6F; i <= 1F; i += 0.1F) {
            poseStack.pushPose();
            poseStack.scale(i, i, i);
            submitSphere(poseStack, collector, CloudRenderTypes.ADDITIVE);
            poseStack.popPose();
        }
        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }

    private void submitSphere(PoseStack poseStack, SubmitNodeCollector collector, RenderType type) {

        int color =
                ARGB.color(
                        255, random.nextInt(0x100), random.nextInt(0x100), random.nextInt(0x100));
        collector.submitCustomGeometry(
                poseStack,
                type,
                (pose, buffer) ->
                        ResourceManager.black_hole.render(
                                pose, buffer, LightTexture.FULL_BRIGHT, color));
    }

    public static final class State extends EntityRenderState {
        float age;
    }
}
