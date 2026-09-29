// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BlockCharger;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.BlockEntityCompat;

public class BlockEntityCharger extends BlockEntityCompat
        implements Synced, GraphResident, FoldedCoreResident, IEnergyHandlerMK2, SyncUnitSchema {

    public static final int DELAY = 20;
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.MAINHAND,
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };
    public int usingTicks;
    public int lastUsingTicks;
    private List<Player> players = List.of();

    @SyncField(units = 1L << 0)
    private long charge;

    @SyncField(units = 1L << 1)
    private boolean particles;

    private int lastOp;

    public BlockEntityCharger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_CHARGER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityCharger be) {
        be.serverTick((ServerLevel) level, pos, state);
        be.advanceArm(level, pos);
    }

    public static void tickClient(
            Level level, BlockPos pos, BlockState state, BlockEntityCharger be) {
        be.advanceArm(level, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        Direction dir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();

        AABB box =
                new AABB(
                                pos.getX() + 0.5,
                                pos.getY(),
                                pos.getZ() + 0.5,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5)
                        .inflate(0.5, 0.0, 0.5);
        players = level.getEntitiesOfClass(Player.class, box);

        charge = 0L;
        for (Player player : players) {
            for (EquipmentSlot slot : SLOTS) {
                charge +=
                        ItemEnergyTransfer.insert(
                                player.getInventory(),
                                inventorySlot(player, slot),
                                Long.MAX_VALUE,
                                true);
            }
        }

        particles = lastOp > 0;
        if (particles) {
            lastOp--;
            if (TickPhase.every(this, 20)) {
                level.playSound(
                        null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
        }

        networkPackNT(50);

        if (particles) {
            double px =
                    pos.getX()
                            + 0.5
                            + (level.getRandom().nextDouble() * 0.0625)
                            + dir.getStepX() * 0.75;
            double py = pos.getY() + 0.1;
            double pz =
                    pos.getZ()
                            + 0.5
                            + (level.getRandom().nextDouble() * 0.0625)
                            + dir.getStepZ() * 0.75;
            level.sendParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    px,
                    py,
                    pz,
                    1,
                    -dir.getStepX() * 0.1,
                    0.0,
                    -dir.getStepZ() * 0.1,
                    0.0);
        }
    }

    private void advanceArm(Level level, BlockPos pos) {
        lastUsingTicks = usingTicks;
        if ((charge > 0L || particles) && usingTicks < DELAY) {
            usingTicks++;
            if (usingTicks == 2) {
                level.playSound(
                        null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
        if ((charge <= 0L && !particles) && usingTicks > 0) {
            usingTicks--;
            if (usingTicks == 4) {
                level.playSound(
                        null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
    }

    @Override
    public long getPower() {
        return 0L;
    }

    @Override
    public void setPower(long power) {}

    @Override
    public long getMaxPower() {
        return charge;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (usingTicks < DELAY || power == 0L) return power;

        long remaining = power;
        for (Player player : players) {
            for (EquipmentSlot slot : SLOTS) {
                if (remaining <= 0L) return 0L;
                Inventory inventory = player.getInventory();
                int index = inventorySlot(player, slot);
                ItemStack stack = inventory.getItem(index);

                long allowance = Math.min(Math.max(remaining / 5L, 1L), remaining);
                long accepted = ItemEnergyTransfer.insert(inventory, index, allowance, simulate);

                if (!simulate && (accepted > 0L || stack.getItem() instanceof IBatteryItem)) {

                    lastOp = 4;
                }
                remaining -= accepted;
            }
        }
        return remaining;
    }

    private static int inventorySlot(Player player, EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND
                ? player.getInventory().selected
                : slot.getIndex(Inventory.INVENTORY_SIZE);
    }

    public boolean acceptsFace(Direction dir) {
        return dir == getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite();
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.charge);
            case 1 -> output.writeBoolean(this.particles);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.charge = input.readLong();
            case 1 -> this.particles = input.readBoolean();
            default -> throw new IllegalArgumentException();
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
