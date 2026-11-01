// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create.motor;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create integration (our own, not in ntm-next or 1.7.10): an electric motor that takes HE from
 * NTM's network and turns a Create shaft on its front face ({@link #FACING}) at a fixed speed. See
 * {@link BlockEntityCreateMotor}.
 */
public class BlockCreateMotor extends DirectionalKineticBlock
        implements IBE<BlockEntityCreateMotor>, ICapabilityBlock {

    public final MotorSpec spec;

    public BlockCreateMotor(Properties properties, MotorSpec spec) {
        super(properties);
        this.spec = spec;
    }

    /** Placed against a shaft (or other kinetic block), the shaft points into it, as Create's motor. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        if (preferred == null
                || (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()))
            return super.getStateForPlacement(context);
        return defaultBlockState().setValue(FACING, preferred);
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return state.hasProperty(FACING) && face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(CreateMotors.MOTOR_BE).powerIn().fe();
    }

    @Override
    public Class<BlockEntityCreateMotor> getBlockEntityClass() {
        return BlockEntityCreateMotor.class;
    }

    @Override
    public BlockEntityType<? extends BlockEntityCreateMotor> getBlockEntityType() {
        return CreateMotors.MOTOR_BE.get();
    }

    /** HE per tick as the item tooltip shows it (see {@code CreateMotorsClient}). */
    static String he(double perTick) {
        return perTick < 0x1.4p3D
                ? String.format(Locale.ROOT, "%.1f", perTick)
                : String.valueOf(Math.round(perTick));
    }
}
