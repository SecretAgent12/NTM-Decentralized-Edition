// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineTapeDrive;
import com.hbm.util.Facing;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderTapeDrive
        implements BlockEntityRenderer<BlockEntityMachineTapeDrive, RenderTapeDrive.State>,
                ConcurrentRenderStateExtraction {
    private static final int DRIVE = ResourceManager.tape_drive.partId("Drive");
    private static final int LIGHT = ResourceManager.tape_drive.partId("Light");
    private static final int RED = 0xFFFF0000;
    private static final int ORANGE = 0xFFFFBF00;
    private static final int GREEN = 0xFF00FF00;

    private final HFRWavefrontObject model = ResourceManager.tape_drive;
    private final RenderType driveType =
            RenderTypes.entityCutoutCull(ResourceManager.tape_drive_tex);
    private final RenderType lightType = FlatCutout.culled(ResourceManager.white_tex);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineTapeDrive be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(BlockMachineHorizontal.FACING), 90);
        for (int i = 0; i < BlockEntityMachineTapeDrive.SLOT_COUNT; i++) {
            state.categories[i] = be.category(i);
        }
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        for (int i = 0; i < BlockEntityMachineTapeDrive.SLOT_COUNT; i++) {
            int category = state.categories[i];
            if (category == BlockEntityMachineTapeDrive.VISUAL_EMPTY) continue;
            pose.pushPose();
            pose.translate(0, 0.25 - 0.5 * (i / 6), 0.3125 - (i % 6) * 0.125);
            collector.submitCustomGeometry(
                    pose,
                    driveType,
                    (p, buffer) -> model.renderPart(p, buffer, state.lightCoords, -1, DRIVE));
            int color =
                    switch (category) {
                        case BlockEntityMachineTapeDrive.VISUAL_BLANK -> ORANGE;
                        case BlockEntityMachineTapeDrive.VISUAL_FILLED -> GREEN;
                        default -> RED;
                    };
            collector.submitCustomGeometry(
                    pose,
                    lightType,
                    (p, buffer) ->
                            model.renderPart(p, buffer, LightTexture.FULL_BRIGHT, color, LIGHT));
            pose.popPose();
        }
        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public final int[] categories = new int[BlockEntityMachineTapeDrive.SLOT_COUNT];
    }
}
