// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDisplay;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKDisplay
        implements BlockEntityRenderer<BlockEntityRBMKDisplay, RenderRBMKDisplay.State>,
                ConcurrentRenderStateExtraction {

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKDisplay be) {
        return new AABB(be.getBlockPos());
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKDisplay be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING), 90);
        state.columns = be.columns.clone();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        poseStack.translate(0.0, 0.5, 0.0);
        poseStack.scale(1F, 8F / 7F, 8F / 7F);
        poseStack.translate(0.0, -0.5, 0.0);

        RBMKColumn[] columns = state.columns;
        if (columns != null) {
            collector.submitCustomGeometry(
                    poseStack,
                    RBMKColumnGrid.PANEL,
                    (pose, buffer) ->
                            RBMKColumnGrid.emit(
                                    pose,
                                    buffer,
                                    columns,
                                    BlockEntityRBMKDisplay.SIZE,
                                    0.28125F,
                                    0.875F,
                                    0.125F * 3F));
        }

        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public RBMKColumn @Nullable [] columns;
    }
}
