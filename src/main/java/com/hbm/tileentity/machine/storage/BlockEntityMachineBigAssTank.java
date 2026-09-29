// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.MachineBigAssTank;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.tileentity.Tiltable;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityMachineBigAssTank extends BlockEntityBarrel implements Tiltable {

    public static final int CAPACITY = 16_000_000;

    private int lastComparator;

    public BlockEntityMachineBigAssTank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIGASSTANK.get(), pos, state, CAPACITY);
    }

    private static boolean graphDiffers(
            ServerLevel level, long key, LevelNodeGraph<PipeData> target) {
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(level, key);
        return graph != null && graph != target;
    }

    private static void ensureNode(LevelNodeGraph<PipeData> graph, long key, Fluid type, int mask) {
        GraphNode<PipeData> node = graph.getNode(key);
        if (node == null) {
            graph.addNode(key, new PipeData(type), mask);
            return;
        }
        if (node.data.fluid() != type) graph.replaceNodeData(key, new PipeData(type));
        graph.updateConnections(key, mask);
    }

    private static void ensureRemoteLink(LevelNodeGraph<PipeData> graph, long a, long b) {
        GraphNode<PipeData> node = graph.getNode(a);
        if (node != null && (node.remoteLinks == null || !node.remoteLinks.contains(b)))
            graph.addRemoteLink(a, b);
    }

    public static void removeVirtualNodes(ServerLevel level, BlockPos corePos, Direction facing) {
        removeVirtualNodes(
                level,
                corePos.asLong(),
                corePos.relative(facing, 6).asLong(),
                corePos.relative(facing.getOpposite(), 6).asLong());
    }

    private static void removeVirtualNodes(ServerLevel level, long core, long front, long back) {
        removeVirtualNode(level, front);
        removeVirtualNode(level, back);
        removeVirtualNode(level, core);
    }

    private static void removeVirtualNode(ServerLevel level, long key) {
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(level, key);
        if (graph != null) graph.removeNode(key);
    }

    @Override
    public void tickServer() {
        checkTilt(Tiltable.TiltType.UNAVOIDABLE, true);
        int comparator = getComparatorPower();
        if (comparator != lastComparator) {
            setChanged();
            ((MachineBigAssTank) getBlockState().getBlock())
                    .updateComparatorOutput((ServerLevel) level, worldPosition);
        }
        lastComparator = comparator;
        super.tickServer();
    }

    @Override
    protected void updateBufferNode(ServerLevel level) {
        long core = worldPosition.asLong();
        Direction facing = getBlockState().getValue(BlockMultiblockCore.FACING);
        long front = worldPosition.relative(facing, 6).asLong();
        long back = worldPosition.relative(facing.getOpposite(), 6).asLong();
        Fluid type = tank.getDeclaredFluid();
        LevelNodeGraph<PipeData> current = FluidPipeGraph.graphAt(level, core);

        if (mode != MODE_BUFFER || type == null || type == Fluids.EMPTY) {
            removeVirtualNodes(level, core, front, back);
            return;
        }

        LevelNodeGraph<PipeData> target = FluidPipeGraph.get(level, type);
        if (current != null && current != target
                || graphDiffers(level, front, target)
                || graphDiffers(level, back, target)) {
            removeVirtualNodes(level, core, front, back);
        }
        ensureNode(target, core, type, 0);
        ensureNode(target, front, type, 1 << facing.ordinal());
        ensureNode(target, back, type, 1 << facing.getOpposite().ordinal());
        ensureRemoteLink(target, core, front);
        ensureRemoteLink(target, core, back);
        target.setSelfEndpoint(core, true);
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        if (isTilted() || mode != MODE_RECEIVE && mode != MODE_BUFFER) return 0L;
        return Math.max(50_000L, (long) (tank.getMaxFill() - tank.getFill()) / 100);
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        if (mode == MODE_BUFFER) return Math.max(50_000L, (long) tank.getFill() / 100);
        if (isTilted() || mode != MODE_SEND) return 0L;
        return Math.max(50_000L, (long) tank.getFill() / 100);
    }

    @Override
    protected void checkFluidInteraction() {
        Fluid type = tank.getTankType();
        if (type != null && NTMFluidProperties.hasTrait(type, FT_Amat.class)) {
            level.destroyBlock(worldPosition, false);
            level.explode(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    10F,
                    true,
                    Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public int getFloorCount() {
        return 4 * 4;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return standardFloor7x7(index);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.bigAssTank");
    }


    // backport: woven trait Tiltable
    private int hbm$tiltChecked;

    private int hbm$tiltValid;

    public boolean isTilted() {
        return getBlockState().getValue(Tiltable.TILTED);
    }

    public void checkTilt(Tiltable.TiltType cfg, boolean extraHeavy) {
        boolean doesTilt =
                cfg == Tiltable.TiltType.UNAVOIDABLE
                        || cfg == Tiltable.TiltType.CONFIG
                                && (MachineData.ENABLE_MACHINE_GRAVITY.get()
                                        || BalanceConfig.enable528MachineGravity);

        if (!doesTilt || getFloorCount() <= 0) {
            hbm$settle(false);
            return;
        }
        if (!TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) return;

        Level level = getLevel();
        if (hbm$tiltChecked >= getFloorCount()) {
            boolean tilted = hbm$tiltValid < hbm$tiltChecked * 0.95;
            if (tilted && !isTilted()) {
                SoundEvent impact = ModSounds.METAL_IMPACT.get();
                level.playSound(null, getBlockPos(), impact, SoundSource.BLOCKS, 3F, 1F);
            }
            hbm$settle(tilted);
            hbm$tiltChecked = 0;
            hbm$tiltValid = 0;
        }

        BlockPos pos = getFloorPosFromIndex(hbm$tiltChecked);
        if (pos == null) return;

        BlockState ground = level.getBlockState(pos);
        hbm$tiltChecked++;

        if (extraHeavy) {

            if (!ground.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) || ground.isSignalSource()) return;

            if (ground.getBlock() instanceof FallingBlock
                    || ground.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || ground.is(BlockTags.WOOL)) return;
            if (ground.getBlock().getExplosionResistance() < Blocks.STONE.getExplosionResistance())
                return;
            hbm$tiltValid++;
            return;
        }
        if (!ground.isFaceSturdy(level, pos, Direction.UP)) return;

        if (ground.getBlock() instanceof FallingBlock) return;
        if (ground.is(ModBlocks.DIRT_DEAD.get())
                || ground.is(ModBlocks.DIRT_OILY.get())
                || ground.is(ModBlocks.STONE_CRACKED.get())) return;
        hbm$tiltValid++;
    }

    private void hbm$settle(boolean tilted) {
        BlockState state = getBlockState();
        if (state.getValue(Tiltable.TILTED) == tilted) return;
        BlockState settled = state.setValue(Tiltable.TILTED, tilted);
        getLevel()
                .setBlock(
                        getBlockPos(),
                        settled,
                        Block.UPDATE_CLIENTS
                                | Block.UPDATE_KNOWN_SHAPE
                                | 0 /* backport: 26.x Block.UPDATE_SKIP_ON_PLACE, no 1.21.1 flag */);
    }
}
