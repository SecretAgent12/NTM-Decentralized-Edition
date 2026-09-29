// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.logic.EntityBomber;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class RenderBomber extends EntityRenderer<EntityBomber, RenderBomber.State>
        implements ConcurrentRenderStateExtraction {

    public RenderBomber(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    private static ResourceLocation texture(int style) {
        return switch (style) {
            case 2 -> ResourceManager.dornier_2_tex;
            case 4 -> ResourceManager.dornier_4_tex;
            case 5 -> ResourceManager.b29_0_tex;
            case 6 -> ResourceManager.b29_1_tex;
            case 7 -> ResourceManager.b29_2_tex;
            case 8 -> ResourceManager.b29_3_tex;
            default -> ResourceManager.dornier_1_tex;
        };
    }

    @Override
    protected boolean affectedByCulling(EntityBomber entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityBomber entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yaw = Mth.rotLerp(partialTicks, entity.renderYawO, entity.renderYaw);
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
        state.style = entity.getStyle();
        state.wobble = (float) Math.sin((entity.tickCount + partialTicks) * 0.05D) * 10F;
        state.light = state.lightCoords;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        pose.mulPose(Axis.ZP.rotationDegrees(90F));
        pose.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        pose.mulPose(Axis.XP.rotationDegrees(state.wobble));
        ResourceLocation texture = texture(state.style);
        RenderType type = WorldRenderPipeline.oneSidedCutout(texture);
        if (state.style >= 0 && state.style <= 4) {
            pose.scale(5F, 5F, 5F);
            pose.mulPose(Axis.YP.rotationDegrees(-90F));
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (p, buffer) -> ResourceManager.dornier.render(p, buffer, state.light, -1));
        } else {
            pose.scale(30F / 3.1F, 30F / 3.1F, 30F / 3.1F);
            pose.mulPose(Axis.YP.rotationDegrees(180F));
            collector.submitCustomGeometry(
                    pose,
                    type,
                    (p, buffer) -> ResourceManager.b29.render(p, buffer, state.light, -1));
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float yaw;
        float pitch;
        float wobble;
        int style;
        int light;
    }
}
