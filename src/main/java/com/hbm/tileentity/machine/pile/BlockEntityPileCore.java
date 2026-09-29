// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.BlockPile.Role;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.PileError;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.weapon.sedna.factory.XFactoryPile;
import com.hbm.lib.DirPos;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import com.hbm.client.ClientSyncRecovery;
import com.hbm.packet.BeSyncTable;
import com.hbm.packet.BlobSynced;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitFrame;
import com.hbm.packet.SyncUnitState;
import com.hbm.packet.SyncWire;
import com.hbm.packet.WireReplayServer;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.packet.toclient.UnitPayload;
import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.VarLong;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.backport.BlockEntityCompat;

public class BlockEntityPileCore extends BlockEntityCompat
        implements GraphResident, FoldedCoreResident, Synced, SyncUnitSchema {

    public PileOrientation orientation = PileOrientation.NEITHER;
    public Direction front = Direction.NORTH;

    public int height;
    public int width;
    public int depth;
    public int left;
    public int right;
    public int up;

    @SyncField(units = 1L)
    public double highestHeat;

    public static final int MAX_HEAT = 800;
    public static boolean meltingDown;
    private @Nullable BoundingBox members;

    public List<PileChannel> fuelChannels = new ArrayList<>();
    public List<PileChannel> ventilationChannels = new ArrayList<>();
    public List<PileChannel> controlChannels = new ArrayList<>();
    public PileSegment[] segments = new PileSegment[0];

    public BlockEntityPileCore(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_CORE.get(), pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        height = input.getIntOr("height", 0);
        width = input.getIntOr("width", 0);
        depth = input.getIntOr("depth", 0);
        left = input.getIntOr("left", 0);
        right = input.getIntOr("right", 0);
        up = input.getIntOr("up", 0);
        orientation = PileOrientation.values()[input.getIntOr("orientation", 2)];
        front =
                Direction.from3DDataValue(
                        input.getIntOr("front", Direction.NORTH.get3DDataValue()));
        members = input.read("members", BoundingBox.CODEC).orElse(null);

        int fuelCount = input.getByteOr("fc", (byte) 0);
        int ventCount = input.getByteOr("vc", (byte) 0);
        int contCount = input.getByteOr("cc", (byte) 0);

        fuelChannels.clear();
        ventilationChannels.clear();
        controlChannels.clear();

        for (int i = 0; i < fuelCount; i++) fuelChannels.add(readChannel(input, "f" + i));
        for (int i = 0; i < ventCount; i++) ventilationChannels.add(readChannel(input, "v" + i));
        for (int i = 0; i < contCount; i++) controlChannels.add(readChannel(input, "c" + i));
        recalculateSegments();
    }

    public @Nullable PileChannel getFuelChannel(BlockPos pos) {
        return getChannel(pos, fuelChannels);
    }

    public @Nullable PileChannel getVentilationChannel(BlockPos pos) {
        return getChannel(pos, ventilationChannels);
    }

    public @Nullable PileChannel getControlChannel(BlockPos pos) {
        return getChannel(pos, controlChannels);
    }

    public @Nullable PileChannel getChannel(BlockPos pos, List<PileChannel> list) {
        for (PileChannel channel : list) if (channel.entry.pos().equals(pos)) return channel;
        return null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("height", height);
        output.putInt("width", width);
        output.putInt("depth", depth);
        output.putInt("left", left);
        output.putInt("right", right);
        output.putInt("up", up);
        output.putInt("orientation", orientation.ordinal());
        output.putInt("front", front.get3DDataValue());
        if (members != null) output.store("members", BoundingBox.CODEC, members);

        int fuelCount = fuelChannels.size();
        int ventCount = ventilationChannels.size();
        int contCount = controlChannels.size();

        output.putByte("fc", (byte) fuelCount);
        output.putByte("vc", (byte) ventCount);
        output.putByte("cc", (byte) contCount);

        for (int i = 0; i < fuelCount; i++) fuelChannels.get(i).writeChannel(output, "f" + i);
        for (int i = 0; i < ventCount; i++)
            ventilationChannels.get(i).writeChannel(output, "v" + i);
        for (int i = 0; i < contCount; i++) controlChannels.get(i).writeChannel(output, "c" + i);
    }

    public BlockEntityPileCore setupSize(
            int up,
            int down,
            int left,
            int right,
            int depth,
            Direction front,
            BoundingBox members) {
        this.height = up + 1 + down;
        this.width = left + 1 + right;
        this.depth = depth;
        this.up = up;
        this.left = left;
        this.right = right;
        this.front = front;
        this.members = members;
        this.segments = new PileSegment[width];
        return this;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        for (PileChannel channel : fuelChannels) channel.ejectAll();
        if (meltingDown) return;
        if (members == null || !(level instanceof ServerLevel server)) return;
        BoundingBox bounds = members;
        members = null;
        for (BlockPos cell : AssembledMembers.members(server, worldPosition, bounds)) {
            server.setBlockAndUpdate(cell, ModBlocks.PILE_BRICK.get().defaultBlockState());
        }
    }

    public List<PileChannel> getChannelList(PileChannelType type) {
        if (type == PileChannelType.FUEL) return this.fuelChannels;
        if (type == PileChannelType.VENTILATION) return this.ventilationChannels;
        return this.controlChannels;
    }

    public boolean drillChannel(BlockPos pos, Direction dir, Player player) {
        Role startRole = level.getBlockState(pos).getValue(BlockPile.ROLE);
        PileChannelType type = PileChannelType.getChannelType(dir, orientation);

        int size =
                type == PileChannelType.CONTROL
                        ? height
                        : type == PileChannelType.FUEL ? depth : width;

        List<PileChannel> list = getChannelList(type);

        if (startRole == Role.FUEL_IN || startRole == Role.AIR_IN || startRole == Role.CONTROL) {
            for (int i = 0; i < list.size(); i++) {
                PileChannel chan = list.get(i);
                if (chan.entry.pos().equals(pos) && chan.entry.dir() == dir) {
                    if (type == PileChannelType.FUEL) chan.ejectAll();
                    list.remove(i);
                    for (int j = 0; j < size; j++) {
                        setRole(pos.relative(dir, j), Role.DUMMY);
                    }
                    recalculateSegments();
                    setChanged();
                    level.playSound(
                            null,
                            pos,
                            SoundEvents.ITEM_BREAK,
                            SoundSource.BLOCKS,
                            1F,
                            0.75F);
                    return true;
                }
            }
        }

        PileError report = new PileError();
        for (int i = 0; i < size; i++) {
            BlockPos probe = pos.relative(dir, i);
            BlockState state = level.getBlockState(probe);

            if (!state.is(ModBlocks.PILE_BLOCK.get())) {
                report.add(probe, Component.translatable("marker.hbm.pile.foreign_block"));
                continue;
            }
            Role role = state.getValue(BlockPile.ROLE);
            if (role == Role.EDGE)
                report.add(probe, Component.translatable("marker.hbm.pile.drill_edge"));
            else if (role == Role.CORE)
                report.add(probe, Component.translatable("marker.hbm.pile.intersect_core"));
            else if (role == Role.CHANNEL)
                report.add(probe, Component.translatable("marker.hbm.pile.intersect_channel"));
            else if (role != Role.DUMMY)
                report.add(probe, Component.translatable("marker.hbm.pile.intersect_io"));
        }

        if (report.send(player)) return false;

        for (int i = 0; i < size; i++) {
            BlockPos probe = pos.relative(dir, i);
            if (i == 0) {
                if (type == PileChannelType.FUEL) setRole(probe, Role.FUEL_IN);
                if (type == PileChannelType.VENTILATION) setRole(probe, Role.AIR_IN);
                if (type == PileChannelType.CONTROL) setRole(probe, Role.CONTROL);
            } else if (i == size - 1) {
                if (type == PileChannelType.FUEL) setRole(probe, Role.FUEL_OUT);
                if (type == PileChannelType.VENTILATION) setRole(probe, Role.AIR_OUT);
                if (type == PileChannelType.CONTROL) setRole(probe, Role.CONTROL);
            } else {
                setRole(probe, Role.CHANNEL);
            }
        }

        list.add(new PileChannel(pos, dir, size, type));

        this.setChanged();
        recalculateSegments();
        level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1F, 1.25F);

        return true;
    }

    private void setRole(BlockPos pos, Role role) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.PILE_BLOCK.get()))
            level.setBlock(pos, state.setValue(BlockPile.ROLE, role), 3);
    }

    public void tickServer() {
        runSimulation();
        handleVentilation();
        handleMeltdown();
        setChanged();
        networkPackNT(25);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        output.writeDouble(highestHeat);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        highestHeat = input.readDouble();
    }

    private void runSimulation() {
        for (PileChannel channel : fuelChannels) {
            if (channel.length <= 0) continue;
            double produced = 0D;
            for (int i = 0; i < channel.rods.length; i++) {
                ItemStack stack = channel.rods[i];
                if (stack.getItem() instanceof ItemPileRodMK2) {
                    double flux =
                            ItemPileRodMK2.getReactivity(
                                    stack, channel.incomingNeutrons / channel.length);
                    produced += flux;
                    channel.heat += flux * ItemPileRodMK2.getHeatPerNeutron(stack);
                    channel.rods[i] = ItemPileRodMK2.react(stack, flux);
                }
            }
            channel.outgoingNeutrons = produced;
            channel.incomingNeutrons = 0D;
        }

        for (PileSegment segment : segments) {
            if (segment == null || segment.type != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel channel : segment.channels) outgoing += channel.outgoingNeutrons;
            for (PileChannel channel : segment.channels) channel.incomingNeutrons += outgoing;
        }

        for (int i = 1; i < segments.length - 1; i++) {
            PileSegment segment = segments[i];
            if (segment == null || segment.type != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel channel : segment.channels) outgoing += channel.outgoingNeutrons;

            double multiplier = 1D;
            for (int j = i - 1; j >= 1; j--) {
                PileSegment neighbor = segments[j];
                if (neighbor == null) continue;
                multiplier *= neighbor.getNeutronMult(depth);
                if (neighbor.type == PileChannelType.FUEL)
                    for (PileChannel channel : neighbor.channels)
                        channel.incomingNeutrons += outgoing * multiplier;
            }

            multiplier = 1D;
            for (int j = i + 1; j < segments.length - 1; j++) {
                PileSegment neighbor = segments[j];
                if (neighbor == null) continue;
                multiplier *= neighbor.getNeutronMult(depth);
                if (neighbor.type == PileChannelType.FUEL)
                    for (PileChannel channel : neighbor.channels)
                        channel.incomingNeutrons += outgoing * multiplier;
            }
        }
    }

    private void handleVentilation() {
        for (PileChannel channel : ventilationChannels) {
            if (channel.air <= 0) continue;
            double airCap = (double) channel.air / PileChannel.MAX_AIR;
            for (PileChannel fuel : fuelChannels) {
                if (Math.abs(fuel.entry.pos().getY() - channel.entry.pos().getY()) <= 1)
                    fuel.heat *= 1D - airCap * 0.05D;
            }
            channel.air -= (int) Math.ceil(airCap * 5D);
            if (!TickPhase.every(this, 3)) continue;

            Direction dir = channel.entry.dir();
            BlockPos entry = channel.entry.pos();
            double x = entry.getX() + 0.5D + dir.getStepX() * (width - 0.375D);
            double y = entry.getY() + 0.5D;
            double z = entry.getZ() + 0.5D + dir.getStepZ() * (width - 0.375D);
            RandomSource rand = level.getRandom();
            CoolingTowerParticleOptions options =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(1F)
                            .setBaseScale((0.125F + rand.nextFloat() * 0.125F) * (float) airCap)
                            .setMaxScale((float) airCap)
                            .setStrafe(0.0025F)
                            .noWind()
                            .setLife(20 + rand.nextInt(30))
                            .setColor(0xa0a0a0)
                            .build();
            ServerLevel server = (ServerLevel) level;
            for (ServerPlayer player : server.players()) {
                if (player.distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                        > 150D * 150D) continue;
                server.sendParticles(player, options, true, x, y, z, 0, 0D, 0D, 0D, 0D);
            }
        }

        for (PileChannel channel : fuelChannels) {
            channel.heat *= 0.999D;
            if (channel.heat < 20D) channel.heat = 20D;
        }
    }

    private void handleMeltdown() {
        highestHeat = 0D;
        for (PileChannel channel : fuelChannels) highestHeat = Math.max(highestHeat, channel.heat);
        if (highestHeat <= MAX_HEAT) return;

        double x = 0D;
        double z = 0D;
        for (PileChannel channel : fuelChannels) {
            Direction dir = channel.entry.dir();
            x += channel.entry.pos().getX() + 0.5D + dir.getStepX() * (channel.length - 1) / 2D;
            z += channel.entry.pos().getZ() + 0.5D + dir.getStepZ() * (channel.length - 1) / 2D;
        }
        x /= fuelChannels.size();
        z /= fuelChannels.size();
        double y = worldPosition.getY() + up;
        ServerLevel server = (ServerLevel) level;
        List<BlockPos> claimed =
                members == null
                        ? List.of()
                        : AssembledMembers.members(server, worldPosition, members);

        meltingDown = true;
        try {
            destroy();
            server.explode(null, x, y, z, 15F, true, Level.ExplosionInteraction.BLOCK);
        } finally {
            for (BlockPos cell : claimed) {
                if (server.getBlockState(cell).is(ModBlocks.PILE_BLOCK.get()))
                    server.setBlockAndUpdate(cell, ModBlocks.PILE_BRICK.get().defaultBlockState());
                AssembledMembers.release(server, cell);
            }
            members = null;
            meltingDown = false;
        }

        for (int i = 0; i < 15; i++) {
            double motionY = level.getRandom().nextDouble() * 0.5D + 1D;
            EntityBulletBaseMK4 debris =
                    new EntityBulletBaseMK4(
                            level,
                            null,
                            XFactoryPile.debris,
                            100F,
                            0.35F,
                            x,
                            y + 1D,
                            z,
                            0D,
                            motionY,
                            0D);
            level.addFreshEntity(debris);
        }
    }

    public void recalculateSegments() {
        segments = new PileSegment[width];
        for (PileChannel channel : fuelChannels) addSegment(channel);
        for (PileChannel channel : controlChannels) addSegment(channel);
    }

    private void addSegment(PileChannel channel) {
        Direction rightDir = front.getClockWise();
        BlockPos entry = channel.entry.pos();
        int index =
                (entry.getX() - worldPosition.getX()) * rightDir.getStepX()
                        + (entry.getZ() - worldPosition.getZ()) * rightDir.getStepZ()
                        + left;
        if (index < 0 || index >= segments.length) return;
        PileSegment segment = segments[index];
        if (segment == null) segments[index] = segment = new PileSegment(channel.type);
        if (segment.type == channel.type) segment.channels.add(channel);
    }

    public void destroy() {
        level.setBlockAndUpdate(worldPosition, ModBlocks.PILE_BRICK.get().defaultBlockState());
    }

    public PileChannel readChannel(ValueInput input, String name) {
        int x = input.getIntOr(name + "_x", 0);
        int y = input.getIntOr(name + "_y", 0);
        int z = input.getIntOr(name + "_z", 0);
        Direction dir = Direction.from3DDataValue(input.getByteOr(name + "_d", (byte) 0));
        PileChannel channel = new PileChannel(new BlockPos(x, y, z), dir);
        if (channel.type == PileChannelType.FUEL) {
            for (int i = 0; i < channel.rods.length; i++) {
                channel.rods[i] =
                        input.read(name + "item" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            }
            channel.heat = input.getDoubleOr(name + "heat", 0D);
            channel.incomingNeutrons = input.getDoubleOr(name + "neutrons", 0D);
        } else if (channel.type == PileChannelType.VENTILATION) {
            channel.air = input.getIntOr(name + "air", 0);
        } else {
            channel.control = input.getDoubleOr(name + "control", 1D);
        }
        return channel;
    }

    public enum PileOrientation {
        NORTH_SOUTH,
        EAST_WEST,
        NEITHER;

        public static PileOrientation getOrientation(Direction dir) {
            if (dir == Direction.NORTH || dir == Direction.SOUTH) return NORTH_SOUTH;
            if (dir == Direction.EAST || dir == Direction.WEST) return EAST_WEST;
            return NEITHER;
        }
    }

    public enum PileChannelType {
        FUEL,
        VENTILATION,
        CONTROL;

        public static PileChannelType getChannelType(
                Direction channelDir, PileOrientation pileOrientation) {

            if (channelDir == Direction.UP || channelDir == Direction.DOWN) {
                return PileChannelType.CONTROL;
            } else if (PileOrientation.getOrientation(channelDir) == pileOrientation) {
                return PileChannelType.FUEL;
            } else {
                return PileChannelType.VENTILATION;
            }
        }
    }

    public class PileChannel {

        public final DirPos entry;
        public final int length;
        public final PileChannelType type;
        public final ItemStack[] rods;
        public double heat;
        public double outgoingNeutrons;
        public double incomingNeutrons;
        public static final int MAX_AIR = 1_000;
        public int air;
        public double control = 1D;

        public PileChannel(BlockPos pos, Direction dir) {
            this.entry = new DirPos(pos, dir);
            this.type = PileChannelType.getChannelType(dir, orientation);
            this.length =
                    type == PileChannelType.CONTROL
                            ? height
                            : type == PileChannelType.FUEL ? depth : width;
            this.rods = new ItemStack[length];
            java.util.Arrays.fill(rods, ItemStack.EMPTY);
        }

        public PileChannel(BlockPos pos, Direction dir, int length, PileChannelType type) {
            this.entry = new DirPos(pos, dir);
            this.type = type;
            this.length = length;
            this.rods = new ItemStack[length];
            java.util.Arrays.fill(rods, ItemStack.EMPTY);
        }

        public void writeChannel(ValueOutput output, String name) {
            output.putInt(name + "_x", entry.pos().getX());
            output.putInt(name + "_y", entry.pos().getY());
            output.putInt(name + "_z", entry.pos().getZ());

            output.putByte(name + "_d", (byte) entry.dir().get3DDataValue());
            if (type == PileChannelType.FUEL) {
                for (int i = 0; i < rods.length; i++) {
                    if (!rods[i].isEmpty())
                        output.store(name + "item" + i, ItemStack.CODEC, rods[i]);
                }
                output.putDouble(name + "heat", heat);
                output.putDouble(name + "neutrons", incomingNeutrons);
            } else if (type == PileChannelType.VENTILATION) {
                output.putInt(name + "air", air);
            } else {
                output.putDouble(name + "control", control);
            }
        }

        public void loadItem(ItemStack incoming) {
            if (incoming.isEmpty()) return;
            ItemStack shifted = incoming;
            for (int i = 0; i < rods.length; i++) {
                if (rods[i].isEmpty()) {
                    rods[i] = shifted;
                    setChanged();
                    return;
                }
                ItemStack previous = rods[i];
                rods[i] = shifted;
                shifted = previous;
            }
            dropItem(shifted, length);
            setChanged();
        }

        public void ejectAll() {
            for (int i = 0; i < rods.length; i++) {
                dropItem(rods[i], length);
                rods[i] = ItemStack.EMPTY;
            }
            setChanged();
        }

        private void dropItem(ItemStack stack, int depth) {
            if (stack.isEmpty()) return;
            ItemPileRodMK2.clearDepletion(stack);
            BlockPos pos = entry.pos().relative(entry.dir(), depth);
            level.addFreshEntity(
                    new ItemEntity(
                            level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack));
        }
    }

    public static class PileSegment {
        public final PileChannelType type;
        public final List<PileChannel> channels = new ArrayList<>();

        public PileSegment(PileChannelType type) {
            this.type = type;
        }

        public double getNeutronMult(int depth) {
            if (type != PileChannelType.CONTROL) return 1D;
            int size = depth - 1;
            if (size < 3) return 0D;
            double total = 0D;
            for (PileChannel channel : channels) total += channel.control;
            return Math.clamp(total / size, 0D, 0.5D);
        }
    }


    // backport: woven trait Synced
    private int hbm$syncDirty = 3;

    private int hbm$syncVersion;

    private int hbm$syncSentVersion;

    private boolean hbm$syncBound;

    private boolean hbm$syncWatched = true;

    private boolean hbm$syncedOnce;

    private CompoundTag hbm$syncInitial;

    private int hbm$syncInitialVersion;

    private long hbm$syncUnits;

    private long hbm$syncClientUnitRevision;

    private SyncUnitState hbm$syncUnitState;

    private UnitPayload hbm$syncUnitPayload;

    public final boolean syncBound() {
        return hbm$syncBound;
    }

    public final void syncChanged(int mask) {
        hbm$syncDirty |= mask;
        if ((mask & 1) != 0 && (Object) this instanceof SyncUnitSchema schema)
            hbm$syncUnits |= schema.syncUnitMask();
    }

    public final void syncUnitsChanged(int mask, long units) {
        if ((mask & 1) != 0 && (Object) this instanceof SyncUnitSchema schema) {
            hbm$syncUnits |= units & schema.syncUnitMask();
        }
        hbm$syncDirty |= mask;
    }

    public final void markSyncEvent() {
        hbm$syncDirty |= 7;
    }

    public final void bindSync(SyncSource owner, int mask) {
        throw new IllegalStateException("A block entity cannot be another snapshot's child");
    }

    public final void unbindSync(SyncSource owner) {
        throw new IllegalStateException("A block entity cannot be another snapshot's child");
    }

    private void hbm$syncBind() {
        if (hbm$syncBound) return;
        SyncBindings.bindFields(this);
        hbm$syncBound = true;
    }

    public void networkPackNT(int range) {
        hbm$syncDiff();
        if (!hbm$syncWatched || (hbm$syncDirty & 1) == 0 && hbm$syncSentVersion == hbm$syncVersion)
            return;
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        ThreadedPayload payload = hbm$syncGated(server);
        if (payload == null) return;
        hbm$syncedOnce = true;
        BeSyncTable.refresh(payload, server, getBlockPos(), range);
    }

    public void networkPackNTTracking() {
        hbm$syncDiff();
        if (!hbm$syncWatched || (hbm$syncDirty & 1) == 0 && hbm$syncSentVersion == hbm$syncVersion)
            return;
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        ThreadedPayload payload = hbm$syncGated(server);
        if (payload != null) {
            PacketWire.sendSyncTracking(payload, server, getBlockPos());
        }
    }

    public final void syncToTracking() {
        hbm$syncDiff();
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        ServerLevel server = (ServerLevel) level;
        ThreadedPayload payload = hbm$syncGated(server, true);
        if (payload != null) PacketWire.sendSyncTracking(payload, server, getBlockPos());
    }

    public void markChanged() {
        Level level = getLevel();
        if (level != null) level.blockEntityChanged(getBlockPos());
    }

    private UnitPayload hbm$syncUnitSnapshot(SyncUnitSchema schema) {
        hbm$syncBind();
        if (!hbm$syncRelays()) {
            if ((hbm$syncDirty & 1) == 0 && hbm$syncUnitPayload != null) return hbm$syncUnitPayload;
            if (hbm$syncUnitState == null) {
                hbm$syncUnitState = new SyncUnitState(schema);
            }
            hbm$syncUnitState.publish(hbm$syncUnits, (hbm$syncDirty & 4) != 0);
        }
        hbm$syncUnits = 0;
        hbm$syncDirty &= ~5;
        SyncUnitState.Snapshot current = hbm$syncUnitState.snapshot();
        if (hbm$syncUnitPayload != null && hbm$syncUnitPayload.revision() == current.revision())
            return hbm$syncUnitPayload;
        UnitPayload next = UnitPayload.snapshot(this, current);
        if (hbm$syncUnitPayload != null) hbm$syncUnitPayload.release();
        else SyncWire.retain(this);
        hbm$syncUnitPayload = next;
        hbm$syncVersion++;
        return next;
    }

    private SyncUnitState hbm$syncCapture(SyncUnitSchema schema) {
        if (!SyncWire.replayCompat()) return null;
        Level level = getLevel();
        if (level == null
                || !level.isClientSide() && !(level.getServer() instanceof WireReplayServer))
            return null;
        if (hbm$syncUnitState == null) hbm$syncUnitState = new SyncUnitState(schema);
        return hbm$syncUnitState;
    }

    private boolean hbm$syncRelays() {
        return SyncWire.replayCompat()
                && hbm$syncUnitState != null
                && hbm$syncUnitState.captured()
                && !getLevel().isClientSide();
    }

    private ThreadedPayload hbm$syncGated(ServerLevel server) {
        return hbm$syncGated(server, false);
    }

    private ThreadedPayload hbm$syncGated(ServerLevel server, boolean includePublished) {
        if (!((Object) this instanceof SyncUnitSchema schema)) return null;
        if (!PacketWire.hasPlayersTracking(server, getBlockPos())) {
            syncWatching(false);
            if (hbm$syncedOnce) {
                hbm$syncedOnce = false;
                BeSyncTable.evict(server, getBlockPos());
            }
            return null;
        }
        ThreadedPayload payload = hbm$syncUnitSnapshot(schema);
        if (!includePublished && hbm$syncSentVersion == hbm$syncVersion) return null;
        hbm$syncSentVersion = hbm$syncVersion;
        payload.retain();
        return payload;
    }

    private void hbm$syncRemove() {
        releaseSync();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$syncRemove();
            com.hbm.tileentity.FoldedCoreResident.onRemove(this); // backport: hbm-compiler FoldedLifecycle
    }

    public final void releaseSync() {
        if ((Object) this instanceof BlobSynced source) source.syncBlob().release();
        hbm$syncClientUnitRevision = 0;
        if (getLevel() instanceof ServerLevel server)
            PacketWire.dropSyncPosition(server, getBlockPos());
        if (hbm$syncedOnce && getLevel() instanceof ServerLevel server) {
            hbm$syncedOnce = false;
            BeSyncTable.evict(server, getBlockPos());
        }
        if (hbm$syncUnitPayload != null) {
            hbm$syncUnitPayload.release();
            hbm$syncUnitPayload = null;
            SyncWire.release(this);
        }
        hbm$syncUnitState = null;
        hbm$syncUnits = 0;
        hbm$syncInitial = null;
        hbm$syncDirty = 3;
        hbm$syncWatched = true;
        if (hbm$syncBound) {
            SyncBindings.unbindFields(this);
            hbm$syncBound = false;
        }
    }

    public final void syncWatching(boolean watched) {
        hbm$syncWatched = watched;
        if ((Object) this instanceof BlobSynced source) source.syncBlob().watching(watched);
    }

    private void hbm$syncLoad(ValueInput input) {
        Optional<ByteBuffer> stored = input.read(SyncWire.KEY, Codec.BYTE_BUFFER);
        if (stored.isPresent()) {
            ByteBuf bytes = Unpooled.wrappedBuffer(stored.get());
            try {
                long revision = VarLong.read(bytes);
                SyncUnitSchema schema = (SyncUnitSchema) (Object) this;
                SyncUnitState capture = hbm$syncCapture(schema);
                if (capture == null) SyncUnitFrame.readUnits(schema, bytes, true);
                else
                    SyncUnitFrame.readUnits(
                            schema,
                            bytes,
                            true,
                            capture,
                            level.isClientSide() ? revision : SyncWire.nextRevision());
                schema.readInitialExtras(bytes);
                if (capture == null || level.isClientSide()) schema.afterInitialSyncUnits();
                else schema.afterRelayedSyncUnits(schema.syncUnitMask(), true);
                if (bytes.isReadable())
                    throw new DecoderException("Trailing machine sync unit data");
                hbm$syncClientUnitRevision =
                        schema.initialMatchesSyncUnits() ? revision : -revision;
            } finally {
                bytes.release();
            }
            if (level != null && level.isClientSide()) {
                int parts = (Object) this instanceof BlobSynced ? 3 : 1;
                ClientSyncRecovery.received(getBlockPos(), parts);
            }
        }
    }

    private boolean hbm$syncRecorded(ValueInput input) {
        if (!(getLevel() instanceof ServerLevel server)
                || !(server.getServer() instanceof WireReplayServer replay)) {
            return false;
        }
        Optional<ByteBuffer> frame = input.read(SyncWire.FRAME_KEY, Codec.BYTE_BUFFER);
        Optional<ByteBuffer> blob = input.read(SyncWire.BLOB_KEY, Codec.BYTE_BUFFER);
        if (frame.isEmpty() && blob.isEmpty()) return false;
        if (frame.isPresent()) {
            if (!((Object) this instanceof SyncUnitSchema schema)) {
                throw new DecoderException("Recorded unit state without a schema");
            }
            if (hbm$syncUnitState == null) hbm$syncUnitState = new SyncUnitState(schema);
            if (hbm$syncUnitState.snapshot() == null)
                hbm$syncUnitState.publish(schema.syncUnitMask());
            ByteBuf bytes = Unpooled.wrappedBuffer(frame.get());
            try {
                boolean full = bytes.readBoolean();
                long units =
                        SyncUnitFrame.readUnits(
                                schema, bytes, full, hbm$syncUnitState, SyncWire.nextRevision());
                if (bytes.isReadable())
                    throw new DecoderException("Trailing machine sync unit data");
                if (schema.afterRelayedSyncUnits(units, false) && replay.skipsViewerTicks()) {
                    replay.resendWhenCaughtUp(this);
                }
            } finally {
                bytes.release();
            }
        }
        if (blob.isPresent()) {
            if (!((Object) this instanceof BlobSynced source)) {
                throw new DecoderException("Recorded blob without a carrier");
            }
            ByteBuf bytes = Unpooled.wrappedBuffer(blob.get());
            try {
                source.syncBlob().ingest(bytes);
            } finally {
                bytes.release();
            }
            source.flushSyncBlob();
        }
        return true;
    }

    public void loadWithComponents(ValueInput input) {
        if (SyncWire.replayCompat() && hbm$syncRecorded(input)) return;
        super.loadWithComponents(input);
        hbm$syncLoad(input);
    }

    public void loadCustomOnly(ValueInput input) {
        super.loadCustomOnly(input);
        hbm$syncLoad(input);
    }

    public final boolean applyUnitSync(long revision, long base, ByteBuf body) {
        if (!((Object) this instanceof SyncUnitSchema schema)) return false;
        if (revision <= Math.abs(hbm$syncClientUnitRevision)) return true;
        if (base != 0 && base != hbm$syncClientUnitRevision) return false;
        SyncUnitState capture = hbm$syncCapture(schema);
        if (capture == null) {
            if (base == 0) SyncUnitFrame.readInitial(schema, body);
            else SyncUnitFrame.read(schema, body);
        } else if (base == 0) {
            SyncUnitFrame.readUnits(schema, body, true, capture, revision);
            schema.afterInitialSyncUnits();
        } else {
            schema.afterSyncUnits(SyncUnitFrame.readUnits(schema, body, false, capture, revision));
        }
        if (body.isReadable()) throw new DecoderException("Trailing machine sync unit data");
        hbm$syncClientUnitRevision = revision;
        return true;
    }

    public final void syncTo(ServerPlayer player) {
        hbm$syncDiff();
        if (!((Object) this instanceof SyncUnitSchema schema)) return;
        if ((SyncWire.initialLayout(getClass()) & SyncWire.INITIAL_EXTRAS) != 0
                && schema.initialMatchesSyncUnits()) {
            ChunkTrackerIndex.beginInitial(player);
            try {
                player.connection.send(ClientboundBlockEntityDataPacket.create(this));
            } finally {
                ChunkTrackerIndex.endInitial();
            }
            return;
        }
        UnitPayload publication = hbm$syncUnitSnapshot(schema);
        ThreadedPayload frame = publication.forBase(0);
        frame.retain();
        PacketWire.sendTo(frame, player);
        ChunkTrackerIndex.snapshotSent(player, this, publication.revision());
        if ((Object) this instanceof BlobSynced source) source.syncBlob().sendFull(player);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        if (!((Object) this instanceof SyncUnitSchema schema))
            return super.getUpdateTag(registries);
        if (SyncWire.replayCompat() && getLevel() != null && getLevel().isClientSide()) {
            return hbm$syncReceivedTag(registries, schema);
        }
        return hbm$syncUnitUpdateTag(registries, schema);
    }

    private CompoundTag hbm$syncReceivedTag(
            HolderLookup.Provider registries, SyncUnitSchema schema) {
        CompoundTag tag = super.getUpdateTag(registries);
        SyncUnitState state = hbm$syncUnitState;
        SyncUnitState.Snapshot received = state == null ? null : state.snapshot();
        if (received == null) return tag;
        ByteBuf scratch = SyncWire.SCRATCH.get();
        scratch.clear();
        VarLong.write(scratch, received.revision());
        received.writeBody(schema.syncUnitMask(), scratch);
        schema.writeInitialExtras(scratch);
        byte[] bytes = new byte[scratch.readableBytes()];
        scratch.getBytes(scratch.readerIndex(), bytes);
        tag.putByteArray(SyncWire.KEY, bytes);
        return tag;
    }

    private CompoundTag hbm$syncUnitUpdateTag(
            HolderLookup.Provider registries, SyncUnitSchema schema) {
        hbm$syncBind();
        UnitPayload publication = hbm$syncUnitSnapshot(schema);
        int layout = SyncWire.initialLayout(getClass());
        boolean published = (layout & SyncWire.INITIAL_REPUBLISHES) != 0 || hbm$syncRelays();
        if (hbm$syncInitial != null
                && hbm$syncInitialVersion == hbm$syncVersion
                && (published && (layout & SyncWire.INITIAL_EXTRAS) == 0
                        || (hbm$syncDirty & 2) == 0 && !hbm$syncRelays())) {
            hbm$syncDirty &= ~2;
            ChunkTrackerIndex.initialSnapshot(this, publication, schema.initialMatchesSyncUnits());
            return hbm$syncInitial;
        }
        ByteBuf scratch = SyncWire.SCRATCH.get();
        scratch.clear();
        VarLong.write(scratch, publication.revision());
        if (published) hbm$syncUnitState.snapshot().writeBody(schema.syncUnitMask(), scratch);
        else SyncUnitFrame.writeInitial(schema, scratch);
        schema.writeInitialExtras(scratch);
        byte[] bytes = new byte[scratch.readableBytes()];
        scratch.getBytes(scratch.readerIndex(), bytes);
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putByteArray(SyncWire.KEY, bytes);
        hbm$syncDirty &= ~2;
        hbm$syncInitialVersion = hbm$syncVersion;
        ChunkTrackerIndex.initialSnapshot(this, publication, schema.initialMatchesSyncUnits());
        return hbm$syncInitial = tag;
    }

    public Packet<ClientGamePacketListener> getUpdatePacket() {
        if ((Object) this instanceof SyncUnitSchema) syncToTracking();
        return null;
    }

    private byte[][] hbm$syncLastUnits;

    /**
     * backport: finds the sync units whose serialised form changed since they were
     * last published and marks them dirty. Replaces hbm-compiler's @SyncField
     * assignment rewriting, which needed the closed tenon toolchain.
     */
    private void hbm$syncDiff() {
        if (!((Object) this instanceof SyncUnitSchema schema)) return;
        long mask = schema.syncUnitMask();
        if (mask == 0) return;
        if (hbm$syncLastUnits == null) hbm$syncLastUnits = new byte[64][];
        io.netty.buffer.ByteBuf scratch = io.netty.buffer.Unpooled.buffer(64);
        long changed = 0;
        try {
            for (int unit = 0; unit < 64; unit++) {
                if ((mask & (1L << unit)) == 0) continue;
                scratch.clear();
                schema.writeSyncUnit(unit, scratch);
                byte[] now = io.netty.buffer.ByteBufUtil.getBytes(scratch);
                if (java.util.Arrays.equals(now, hbm$syncLastUnits[unit])) continue;
                hbm$syncLastUnits[unit] = now;
                changed |= 1L << unit;
            }
        } finally {
            scratch.release();
        }
        if (changed != 0) syncUnitsChanged(3, changed);
    }

    // backport: hbm-compiler FoldedLifecycle
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        com.hbm.tileentity.FoldedCoreResident.onLoad(this);
    }
}
