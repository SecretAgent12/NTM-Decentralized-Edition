// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.Floodlight;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityFloodlight;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFloodlight
        implements BlockEntityRenderer<BlockEntityFloodlight, RenderFloodlight.State>,
                ConcurrentRenderStateExtraction {
    private static final int LIGHTS = ResourceManager.floodlight.partId("Lights");
    private static final int LAMPS = ResourceManager.floodlight.partId("Lamps");

    private static final RenderType BODY_TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.floodlight_tex);

    private static final RenderType LAMPS_LIT_TYPE = FlatCutout.of(ResourceManager.floodlight_tex);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityFloodlight be) {
        return new AABB(be.getBlockPos()).inflate(1.0D);
    }

    @Override
    public void extractRenderState(
            BlockEntityFloodlight be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(Floodlight.FACING);
        state.rotation = be.rotation;
        state.isOn = be.isOn;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int meta = state.facing;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        switch (meta) {
            case 0, 6 -> poseStack.mulPose(Axis.XP.rotationDegrees(180F));
            case 1, 7 -> {}
            case 2 -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
            }
            case 3 -> poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            case 4 -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90F));
            }
            case 5 -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(270F));
            }
            default -> throw new IllegalStateException("Unexpected floodlight facing: " + meta);
        }
        poseStack.translate(0.0, -0.5, 0.0);
        if (meta != 0 && meta != 1) poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        float rotation = state.rotation;
        if (meta == 0 || meta == 6) rotation -= 90F;
        if (meta == 1 || meta == 7) rotation += 90F;
        poseStack.translate(0.0, 0.5, 0.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(0.0, -0.5, 0.0);

        int lightsLight = state.lightCoords;
        int lampsLight = state.isOn ? LightTexture.FULL_BRIGHT : state.lightCoords;
        int lampsColor = state.isOn ? -1 : 0xFF404040;
        collector.submitCustomGeometry(
                poseStack,
                BODY_TYPE,
                (pose, buffer) ->
                        ResourceManager.floodlight.renderPart(
                                pose, buffer, lightsLight, -1, LIGHTS));
        collector.submitCustomGeometry(
                poseStack,
                state.isOn ? LAMPS_LIT_TYPE : BODY_TYPE,
                (pose, buffer) ->
                        ResourceManager.floodlight.renderPart(
                                pose, buffer, lampsLight, lampsColor, LAMPS));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public int facing = 1;
        public float rotation;
        public boolean isOn;
    }
}
