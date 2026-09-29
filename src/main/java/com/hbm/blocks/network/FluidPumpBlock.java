// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.network.BlockEntityFluidPump;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.hbm.backport.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.BlockCompat;
import com.hbm.backport.InteractionResults;

// backport: @RefreshesNeighborState(be = BlockEntityFluidPump.class, calling = "refreshRedstone") woven below
public class FluidPumpBlock extends BlockCompat implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static Consumer<BlockEntityFluidPump> OPEN_GUI = be -> {};

    public FluidPumpBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {

        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFluidPump(pos, state);
    }

    @Override
    protected InteractionResult backport$useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        if (stack.getItem() instanceof FluidIdentifierItem) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (level.getBlockEntity(pos) instanceof BlockEntityFluidPump pump) {
                FluidIdentifierData data =
                        stack.getOrDefault(
                                ModDataComponents.FLUID_IDENTIFIER.get(),
                                FluidIdentifierData.EMPTY);
                Fluid type = data.primary();
                if (type != null && type != Fluids.EMPTY) {
                    pump.type = type;
                    pump.setChanged();
                    player.sendSystemMessage(
                            Component.translatable(
                                    "hbm.message.pumpSet",
                                    NTMFluidProperties.getDisplayName(type)));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResults.fromVanilla(useWithoutItem(state, level, pos, player, hit));
    }

    @Override
    protected InteractionResult backport$useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()
                && level.getBlockEntity(pos) instanceof BlockEntityFluidPump pump) {
            OPEN_GUI.accept(pump);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityFluidPump pump)) return;
        String typeName =
                pump.type == null
                        ? Component.translatable("hbmfluid.none").getString()
                        : NTMFluidProperties.clientName(pump.type);
        info.title(getName().getString(), 0xffff00, 0x404000)
                .line(
                        ChatFormatting.GREEN
                                + "-> "
                                + ChatFormatting.RESET
                                + typeName
                                + " ("
                                + pump.pressure
                                + " PU): "
                                + String.format(Locale.US, "%,d", pump.rate)
                                + "mB/t"
                                + ChatFormatting.RED
                                + " ->")
                .line("Priority: " + ChatFormatting.YELLOW + pump.priority.name());
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut().selfProvided();
    }

    @Override
    public void declareExtraCaps(RegistryHandle<? extends Block> self) {

        Services.CAPS.registerProvider(
                FluidCaps.PROVIDER,
                ModBlockEntities.PIPE_PUMP,
                (be, face) -> face.side() == be.output() && be.carries(face.fluid()) ? be : null);
        Services.CAPS.registerProvider(
                FluidCaps.RECEIVER,
                ModBlockEntities.PIPE_PUMP,
                (be, face) -> face.side() == be.intake() && be.carries(face.fluid()) ? be : null);
    }

    // backport: woven by hbm-compiler NeighborEdge (@RefreshesNeighborState)
    @Override
    protected void neighborChanged(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, com.hbm.backport.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof BlockEntityFluidPump) {
            ((BlockEntityFluidPump) hbm$be).refreshRedstone();
        }
    }

    @Override
    protected void onPlace(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof BlockEntityFluidPump) {
            ((BlockEntityFluidPump) hbm$be).refreshRedstone();
        }
    }
}
