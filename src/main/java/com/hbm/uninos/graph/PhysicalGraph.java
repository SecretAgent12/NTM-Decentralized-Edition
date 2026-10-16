// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.uninos.graph;

import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerGraphProvider;
import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.FluidPipeGraphProvider;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.CapabilityDependentShape;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.PhysicalConnector;
import com.hbm.util.ChunkUtil;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.platform.NeoForgeServerEvents;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import com.hbm.backport.transfer.Capabilities;
import org.jspecify.annotations.Nullable;

public final class PhysicalGraph {
    private static final Batch UNBOUND = new Batch(null);

    private PhysicalGraph() {}

    public static boolean powerContact(LevelReader level, BlockPos pos, Direction side) {
        if (!(level instanceof Level world)) return false;
        BlockState state = ChunkUtil.blockStateIfLoaded(world, pos);
        if (state != null && state.getBlock() instanceof PhysicalConnector connector) {
            return connector.hasPowerContact(world, pos, state, side);
        }
        if (world instanceof ServerLevel server) {
            var graph = PowerGraph.TYPE.loaded(server);
            if (graph != null && graph.containsCell(pos.asLong()))
                return graph.isOpenAt(pos.asLong(), side);
        }
        return powerEndpoint(world, pos, state, side);
    }

    public static boolean fluidContact(
            LevelReader level, BlockPos pos, Direction side, Fluid fluid) {
        if (!(level instanceof Level world)) return false;
        BlockState state = ChunkUtil.blockStateIfLoaded(world, pos);
        if (state != null && state.getBlock() instanceof PhysicalConnector connector) {
            return connector.hasFluidContact(world, pos, state, side, fluid);
        }
        if (world instanceof ServerLevel server) {
            for (LevelNodeGraph.Kind<PipeData> kind : FluidPipeGraph.KINDS) {
                var graph = kind.loaded(server);
                if (graph == null || !(graph.dataAt(pos.asLong()) instanceof PipeData data))
                    continue;
                if (data.fluid() == fluid) return graph.isOpenAt(pos.asLong(), side);
                if (!FluidPipeGraph.isSmoke(data.fluid()) && !FluidPipeGraph.isSmoke(fluid))
                    return false;
            }
        }
        return fluidEndpoint(world, pos, state, side, fluid);
    }

    public static boolean powerEndpoint(
            Level world, BlockPos pos, @Nullable BlockState state, Direction side) {
        if (state == null) return false;
        if (EnergyCaps.PROVIDER.getCapability(world, pos, null, null, side) != null
                || EnergyCaps.RECEIVER.getCapability(world, pos, null, null, side) != null)
            return true;
        return !NtmCapabilities.ownsEnergyExposure(state)
                && Capabilities.Energy.BLOCK.getCapability(world, pos, null, null, side) != null;
    }

    public static boolean fluidEndpoint(
            Level world, BlockPos pos, @Nullable BlockState state, Direction side, Fluid fluid) {
        if (state == null) return false;
        FluidFace face = FluidFace.of(side, fluid);
        var provider = FluidCaps.PROVIDER.getCapability(world, pos, null, null, face);
        if (provider != null && provider.canProvideFluid(fluid)) return true;
        var receiver = FluidCaps.RECEIVER.getCapability(world, pos, null, null, face);
        if (receiver != null && receiver.canReceiveFluid(fluid)) return true;
        return !NtmCapabilities.ownsFluidExposure(state)
                && Capabilities.Fluid.BLOCK.getCapability(world, pos, null, null, side) != null;
    }

    public static BlockState predictArm(
            BlockState state, BooleanProperty arm, BlockState neighbour, boolean joins) {
        if (joins) return state.setValue(arm, true);
        return neighbour.isAir() ? state.setValue(arm, false) : state;
    }

    public static void redraw(BlockState state, ServerLevel level, BlockPos pos) {
        Batch batch = level.hbm$physicalBatch();
        if (batch != null) {
            batch.faces.mergeInt(pos.asLong(), 0x3f, (a, b) -> a | b);
            return;
        }
        BlockState drawn = state;
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.VALUES) {
            neighbour.setWithOffset(pos, dir);
            BlockState next = ChunkUtil.blockStateIfLoaded(level, neighbour);
            if (next != null) {
                drawn =
                        drawn.updateShape(dir, next, level, pos, neighbour);
            }
        }
        publish(level, pos, state, drawn);
    }

    public static void refreshAround(ServerLevel level, BlockPos changed) {
        BlockState source = ChunkUtil.blockStateIfLoaded(level, changed);
        if (source == null) return;
        refreshAround(level, changed, source);
    }

    static void conductorChanged(ServerLevel level, long key, IGraphProvider<?> provider) {
        if (provider != PowerGraphProvider.INSTANCE && provider != FluidPipeGraphProvider.INSTANCE)
            return;
        BlockPos changed = BlockPos.of(key);
        BlockState source = ChunkUtil.blockStateIfLoaded(level, changed);
        if (source == null) return;
        if (provider == PowerGraphProvider.INSTANCE
                && source.getBlock() instanceof PhysicalConnector) return;
        if (source.getBlock() instanceof CapabilityDependentShape) redraw(source, level, changed);
        refreshAround(level, changed, source);
    }

    private static void refreshAround(ServerLevel level, BlockPos changed, BlockState source) {
        Batch batch = level.hbm$physicalBatch();
        if (batch != null) {
            batch.around(changed.asLong());
            return;
        }
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.VALUES) {
            neighbour.setWithOffset(changed, direction);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, neighbour);
            if (state == null || !(state.getBlock() instanceof CapabilityDependentShape)) continue;
            BlockState drawn =
                    state.updateShape(direction.getOpposite(), source, level, neighbour, changed);
            publish(level, neighbour, state, drawn);
        }
    }

    public static Batch begin(@Nullable ServerLevel level) {
        if (level == null) return UNBOUND;
        Batch batch = level.hbm$physicalBatch();
        if (batch == null) level.hbm$physicalBatch(batch = new Batch(level));
        batch.depth++;
        return batch;
    }

    public static final class Batch implements AutoCloseable {
        private final @Nullable ServerLevel level;
        private final Long2IntOpenHashMap faces = new Long2IntOpenHashMap();
        private int depth;

        private Batch(@Nullable ServerLevel level) {
            this.level = level;
        }

        private void around(long key) {
            for (Direction direction : Direction.VALUES) {
                long neighbour = LevelNodeGraph.offset(key, direction);
                int face = 1 << direction.getOpposite().ordinal();
                faces.mergeInt(neighbour, face, (a, b) -> a | b);
            }
        }

        @Override
        public void close() {
            if (level == null) return;
            assert depth > 0;
            if (--depth != 0) return;
            level.hbm$physicalBatch(null);
            for (var entry : faces.long2IntEntrySet()) {
                BlockPos position = BlockPos.of(entry.getLongKey());
                BlockState state = ChunkUtil.blockStateIfLoaded(level, position);
                if (state == null || !(state.getBlock() instanceof CapabilityDependentShape))
                    continue;
                BlockState drawn = state;
                int mask = entry.getIntValue();
                while (mask != 0) {
                    int face = Integer.numberOfTrailingZeros(mask);
                    mask &= mask - 1;
                    Direction direction = Direction.VALUES[face];
                    BlockPos neighbour = position.relative(direction);
                    BlockState next = ChunkUtil.blockStateIfLoaded(level, neighbour);
                    if (next != null)
                        drawn =
                                drawn.updateShape(direction, next, level, position, neighbour);
                }
                publish(level, position, state, drawn);
            }
        }
    }

    /**
     * backport-fix: BF-097 a chunk's block entities are published inside its FULL task (registerTickContainerInLevel),
     * and a multiblock there redraws the connectors around it. Level.setBlock in that task let Lithium's hopper
     * update hook (updateHopperOnUpdateSuppression -> getLoadedExistingBlockEntity -> Level.getChunk) wait for a
     * chunk whose loading was the very task running it: the server thread parked forever. Redraws asked for while a
     * chunk of the level is loading wait for the end of the server tick and are redrawn from the then current
     * neighbours; nothing else changes.
     */
    private static final Map<ServerLevel, LongOpenHashSet> DEFERRED = new WeakHashMap<>();

    public static void init() {
        NeoForgeServerEvents.onServerTickPost(PhysicalGraph::flushDeferred);
        NeoForgeServerEvents.onServerStopping(server -> DEFERRED.clear());
    }

    private static void flushDeferred(MinecraftServer server) {
        if (DEFERRED.isEmpty()) return;
        for (Map.Entry<ServerLevel, LongOpenHashSet> entry : DEFERRED.entrySet()) {
            ServerLevel level = entry.getKey();
            LongOpenHashSet pending = entry.getValue();
            if (pending.isEmpty() || ChunkPublication.loadingAny(level)) continue;
            long[] keys = pending.toLongArray();
            pending.clear();
            for (long key : keys) {
                BlockPos pos = BlockPos.of(key);
                BlockState state = ChunkUtil.blockStateIfLoaded(level, pos);
                if (state != null && state.getBlock() instanceof CapabilityDependentShape)
                    redraw(state, level, pos);
            }
        }
    }

    private static void publish(
            ServerLevel level, BlockPos pos, BlockState state, BlockState drawn) {
        if (drawn == state) return;
        if (ChunkPublication.loadingAny(level)) {
            DEFERRED.computeIfAbsent(level, l -> new LongOpenHashSet()).add(pos.asLong());
            return;
        }
        level.setBlock(pos, drawn, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
}
