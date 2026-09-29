// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.pile.BlockEntityPileLoader;
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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPileLoader
        implements BlockEntityRenderer<BlockEntityPileLoader, RenderPileLoader.State>,
                ConcurrentRenderStateExtraction {
    private static final int LEVER = ResourceManager.pile_loader.partId("Lever");
    private static final int SLIDER = ResourceManager.pile_loader.partId("Slider");
    private static final int ROD = ResourceManager.pile_loader.partId("Rod");
    private static final RenderType MATERIAL =
            RenderTypes.entitySolid(ResourceManager.pile_loader_tex);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityPileLoader be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(BlockPileDevice.FACING);
        state.extension = be.lastExtension + (be.extension - be.lastExtension) * partialTicks;
        state.hasRod = !be.getItem(0).isEmpty();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));

        poseStack.pushPose();
        poseStack.translate(-0.1875D, 0.5D, 0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) state.extension * 90F));
        poseStack.translate(0.1875D, -0.5D, 0D);
        collector.submitCustomGeometry(
                poseStack,
                MATERIAL,
                (pose, buffer) ->
                        ResourceManager.pile_loader.renderPart(
                                pose, buffer, state.lightCoords, -1, LEVER));
        poseStack.popPose();

        poseStack.translate(state.extension * -0.5D, 0D, 0D);
        collector.submitCustomGeometry(
                poseStack,
                MATERIAL,
                (pose, buffer) ->
                        ResourceManager.pile_loader.renderPart(
                                pose, buffer, state.lightCoords, -1, SLIDER));
        if (state.hasRod)
            collector.submitCustomGeometry(
                    poseStack,
                    MATERIAL,
                    (pose, buffer) ->
                            ResourceManager.pile_loader.renderPart(
                                    pose, buffer, state.lightCoords, -1, ROD));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        double extension;
        boolean hasRod;
    }
}
