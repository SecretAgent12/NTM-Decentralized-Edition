// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.BlockCargoElevator;
import com.hbm.tileentity.machine.BlockEntityCargoElevator;
import com.hbm.backport.client.rendertype.BlendFunction;
import com.hbm.backport.client.rendertype.ColorTargetState;
import com.hbm.backport.client.rendertype.DepthStencilState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.hbm.backport.client.rendertype.BlendFactor;
import com.hbm.backport.client.rendertype.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class CargoElevatorOutline {

    public static final RenderPipeline PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/cargo_elevator_outline")
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withDepthStencilState(
                                    new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)));
    private static final RenderType TYPE =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "cargo_elevator_outline", RenderSetup.builder(PIPELINE).createRenderSetup());

    private CargoElevatorOutline() {}

    public static boolean submit(
            PoseStack pose, SubmitNodeCollector collector, LevelRenderState state) {
        var level = Minecraft.getInstance().level;
        if (level == null || state.blockOutlineRenderState == null) return false;
        BlockPos hit = state.blockOutlineRenderState.pos();
        if (!(level.getBlockState(hit).getBlock() instanceof BlockCargoElevator block))
            return false;
        BlockEntityCargoElevator elevator = block.elevator(level, hit);
        if (elevator == null) return true;
        BlockPos core = elevator.getBlockPos();
        Vec3 camera = state.cameraRenderState.pos;
        pose.pushPose();
        pose.translate(core.getX() - camera.x, core.getY() - camera.y, core.getZ() - camera.z);
        for (AABB box : elevator.boxes()) {
            collector.submitShapeOutline(
                    pose, Shapes.create(box.inflate(0.002F)), TYPE, 0x66000000, 2F, false);
        }
        pose.popPose();
        return true;
    }
}
