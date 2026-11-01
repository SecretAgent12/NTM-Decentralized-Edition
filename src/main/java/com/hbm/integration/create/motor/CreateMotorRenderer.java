// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create.motor;

import com.hbm.lib.Library;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create integration (our own, not in ntm-next or 1.7.10): draws an electric motor's turning shaft.
 * The motor's body is a plain block model (blockstate); the shaft stub is a separate partial model,
 * modelled facing north like the body, turned here to the motor's facing the way the blockstate
 * turns the body, then spun about the shaft axis with Create's kinetic angle.
 *
 * <p>Client only: registered from {@code NuclearTechNeoForgeClient}, with Create loaded.
 */
public final class CreateMotorRenderer implements BlockEntityRenderer<BlockEntityCreateMotor> {

    public static final PartialModel SHAFT = PartialModel.of(Library.id("block/create_motor_shaft"));
    public static final PartialModel SHAFT_DESH =
            PartialModel.of(Library.id("block/create_motor_desh_shaft"));

    public CreateMotorRenderer(BlockEntityRendererProvider.Context context) {}

    /** Loads the partial models above early enough for Flywheel to bake them. */
    public static void init() {}

    @Override
    public void render(
            BlockEntityCreateMotor motor,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        BlockState state = motor.getBlockState();
        if (!(state.getBlock() instanceof BlockCreateMotor block)) return;
        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
        Direction.Axis axis = facing.getAxis();

        SuperByteBuffer shaft =
                CachedBuffers.partial(block.spec == MotorSpec.DESH ? SHAFT_DESH : SHAFT, state);
        // transforms apply last-call-first: the stub is turned to the facing, then spun about the axis
        {
            float angle = KineticBlockEntityRenderer.getAngleForBe(motor, motor.getBlockPos(), axis);
            KineticBlockEntityRenderer.kineticRotationTransform(shaft, motor, axis, angle, light);
        }
        shaft.rotateCenteredDegrees(-yRotation(facing), Direction.Axis.Y);
        shaft.rotateCenteredDegrees(-xRotation(facing), Direction.Axis.X);
        shaft.renderInto(poseStack, buffers.getBuffer(RenderType.solid()));
    }

    /** The blockstate's {@code y} for a north-facing model (clockwise seen from above). */
    private static float yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90F;
            case SOUTH -> 0x1.68p7F;
            case WEST -> 270F;
            default -> 0F;
        };
    }

    /** The blockstate's {@code x} for a north-facing model. */
    private static float xRotation(Direction facing) {
        return switch (facing) {
            case UP -> 270F;
            case DOWN -> 90F;
            default -> 0F;
        };
    }
}
