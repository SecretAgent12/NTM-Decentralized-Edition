// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachinePWRController;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.items.machine.ItemPWRPrinter;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.MarkerPayload;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.BlockCompat;
import com.hbm.backport.SubLevelRebuild;

public class MachinePWRController extends BlockCompat
        implements ITickingBlock, ICapabilityBlock, SubLevelRebuild.Owner {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int MAX_SIZE = 4096;

    public MachinePWRController(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    static boolean isValidCore(Block b) {
        return b == ModBlocks.PWR_FUEL.get()
                || b == ModBlocks.PWR_CONTROL.get()
                || b == ModBlocks.PWR_CHANNEL.get()
                || b == ModBlocks.PWR_HEATEX.get()
                || b == ModBlocks.PWR_HEATSINK.get()
                || b == ModBlocks.PWR_NEUTRON_SOURCE.get();
    }

    private static boolean isValidCasing(Block b) {
        return b == ModBlocks.PWR_CASING.get()
                || b == ModBlocks.PWR_REFLECTOR.get()
                || b == ModBlocks.PWR_PORT.get();
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
        return new BlockEntityMachinePWRController(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PWR_CONTROLLER).fluidIn().fluidOut();
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
        if (stack.getItem() instanceof ItemPWRPrinter) return InteractionResult.PASS;
        return super.backport$useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult backport$useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachinePWRController controller))
            return InteractionResult.PASS;

        if (!controller.assembled) {
            assemble(level, pos, state, player);
        } else {
            IGUIProvider.openBlockMenu(player, controller, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private void assemble(Level level, BlockPos pos, BlockState state, Player player) {
        Scan scan = scan(level, pos, state, false);

        if (!scan.errors.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                PacketWire.sendTo(new MarkerPayload(0xff0000, 5000, 128.0, scan.errors), sp);
            }
            return;
        }

        claim(level, pos, scan);
    }

    /** The structure behind the controller, as the assembly finds it. */
    private record Scan(
            Map<BlockPos, BlockState> assembly,
            Map<BlockPos, BlockState> fuelRods,
            Map<BlockPos, Component> errors) {}

    /**
     * Walks the structure from the block behind the controller. With {@code shells} (rebuilding
     * an assembled reactor, BF-074) a PWR shell counts as the block it stands for; the first
     * assembly sees only the original blocks, as in ntm-next.
     */
    private static Scan scan(Level level, BlockPos pos, BlockState state, boolean shells) {
        Map<BlockPos, BlockState> assembly = new HashMap<>();
        Map<BlockPos, BlockState> fuelRods = new HashMap<>();
        int sources = 0;

        assembly.put(pos, state);
        Direction dir = state.getValue(FACING).getOpposite();

        ArrayDeque<BlockPos> stack = new ArrayDeque<>();
        stack.push(pos.relative(dir));

        Map<BlockPos, Component> errors = new LinkedHashMap<>();
        while (!stack.isEmpty()) {
            BlockPos p = stack.pop();
            if (assembly.containsKey(p)) continue;
            if (assembly.size() >= MAX_SIZE) {
                errors.put(p, Component.translatable("marker.hbm.max_size"));
                continue;
            }

            BlockState s = level.getBlockState(p);
            if (shells && s.getBlock() instanceof BlockPWR) s = s.getValue(BlockPWR.PART).original();
            Block b = s.getBlock();

            if (isValidCasing(b)) {
                assembly.put(p, s);
                continue;
            }
            if (isValidCore(b)) {
                assembly.put(p, s);
                if (b == ModBlocks.PWR_FUEL.get()) fuelRods.put(p, s);
                if (b == ModBlocks.PWR_NEUTRON_SOURCE.get()) sources++;
                stack.push(p.north());
                stack.push(p.south());
                stack.push(p.below());
                stack.push(p.above());
                stack.push(p.west());
                stack.push(p.east());
                continue;
            }
            errors.put(p, Component.translatable("marker.hbm.pwr.non_reactor"));
        }

        if (fuelRods.isEmpty()) {
            errors.put(pos, Component.translatable("marker.hbm.pwr.fuel_required"));
        }
        if (sources == 0) {
            errors.put(pos, Component.translatable("marker.hbm.pwr.source_required"));
        }
        return new Scan(assembly, fuelRods, errors);
    }

    private static void claim(Level level, BlockPos pos, Scan scan) {
        List<BlockPos> members = new ArrayList<>(scan.assembly.size());
        for (Map.Entry<BlockPos, BlockState> entry : scan.assembly.entrySet()) {
            BlockPos partPos = entry.getKey();
            if (partPos.equals(pos)) continue;
            BlockState shell = BlockPWR.shellOf(entry.getValue());
            if (level.getBlockState(partPos) != shell) level.setBlock(partPos, shell, 3);
            members.add(partPos);
        }
        AssembledMembers.assemble((ServerLevel) level, pos, members);

        if (level.getBlockEntity(pos) instanceof BlockEntityMachinePWRController controller) {
            controller.setup(scan.assembly, scan.fuelRods);
            controller.assembled = true;
            controller.markChanged();
        }
    }

    /** backport-fix: BF-077 — see SubLevelRebuild.Owner#structure (Aeronautics assembler, SimulatedCompat). */
    @Override
    public List<BlockPos> structure(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachinePWRController controller)
                || !controller.assembled) return List.of();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MachinePWRController)) return List.of();
        return List.copyOf(scan(level, pos, state, true).assembly.keySet());
    }

    /**
     * backport-fix: BF-074 — after a Sable move or a reload on a contraption the shells have no
     * owner and the controller's stored positions are stale: find the shells again and claim them
     * (positions, ports, rods and bounds are recomputed by setup). A reactor that didn't come
     * through whole is taken apart instead.
     */
    @Override
    public void rebuild(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachinePWRController controller)
                || !controller.assembled) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MachinePWRController)) return;

        Scan scan = scan(level, pos, state, true);
        if (scan.errors.isEmpty()) {
            claim(level, pos, scan);
            return;
        }

        controller.assembled = false;
        controller.markChanged();
        for (BlockPos partPos : scan.assembly.keySet()) {
            BlockState shell = level.getBlockState(partPos);
            if (shell.getBlock() instanceof BlockPWR) {
                AssembledMembers.release(level, partPos);
                level.setBlock(partPos, shell.getValue(BlockPWR.PART).original(), UPDATE_ALL);
            }
        }
    }
}
