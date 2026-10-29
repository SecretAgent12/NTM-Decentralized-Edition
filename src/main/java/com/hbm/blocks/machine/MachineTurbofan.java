// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineTurbofan;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MachineTurbofan extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock, IToolable, ILookOverlay {

    private static final boolean SABLE =
            net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded("sable");

    public MachineTurbofan(Properties props) {
        super(props);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineTurbofan(pos, state);
    }

    /** backport: the screwdriver switches the control mode (classic / throttle). */
    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        BlockEntityMachineTurbofan fan = fan(level, pos);
        if (fan == null) return false;
        if (level.isClientSide()) return true;
        fan.cycleControlMode();
        fan.networkPackNT(150);
        player.displayClientMessage(modeLine(fan.controlMode), true);
        return true;
    }

    /** backport: what the engine does: control mode, signal, thrust (with Sable), power, fuel. */
    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockEntityMachineTurbofan fan = fan(level, pos);
        if (fan == null) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(modeLine(fan.controlMode), 0x55FFFF);
        info.line(Component.translatable(getDescriptionId() + ".signal", fan.signal));
        if (SABLE) {
            info.line(
                    Component.translatable(
                            getDescriptionId() + ".thrust",
                            Math.round(fan.currentThrust()),
                            Math.round(BlockEntityMachineTurbofan.maxThrust(fan.afterburner))));
        }
        info.line(Component.translatable(getDescriptionId() + ".output", fan.outputRate));
        info.line(
                Component.translatable(
                        getDescriptionId() + ".fuel", String.format("%.2f", fan.burnRate)));
    }

    /** The turbofan whose core or (folded) cell is at {@code pos}. */
    private static @Nullable BlockEntityMachineTurbofan fan(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BlockEntityMachineTurbofan fan) return fan;
        BlockPos core =
                level.isClientSide()
                        ? MultiblockSurface.clientCoreOf(level, pos)
                        : MultiblockSurface.coreOfAny(level, pos, level.getBlockState(pos));
        return core != null && level.getBlockEntity(core) instanceof BlockEntityMachineTurbofan fan
                ? fan
                : null;
    }

    private Component modeLine(int mode) {
        return Component.translatable(
                getDescriptionId()
                        + (mode == BlockEntityMachineTurbofan.MODE_THROTTLE
                                ? ".control.throttle"
                                : ".control.classic"));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.MACHINE_TURBOFAN)
                .powerOut()
                .fluidIn()
                .fluidOut()
                .fe();
    }
}
