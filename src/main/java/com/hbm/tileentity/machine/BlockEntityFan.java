// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.MachineFan;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.IBlowable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.backport.BlockEntityCompat;

public class BlockEntityFan extends BlockEntityCompat
        implements Synced, GraphResident, FoldedCoreResident, SyncUnitSchema {

    public float spin;
    public float prevSpin;

    @SyncField(units = 1L << 0)
    public boolean falloff = true;

    @SyncField(units = 1L << 1)
    public boolean suck = false;

    public BlockEntityFan(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FAN.get(), pos, state);
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (!state.getValue(MachineFan.POWERED)) prevSpin = spin;
    }

    public void tick() {

        this.prevSpin = this.spin;

        Direction dir = getBlockState().getValue(MachineFan.FACING);

        int range = 10;
        int effRange = 0;
        double push = 0.1;

        for (int i = 1; i <= range; i++) {
            BlockPos probe = worldPosition.relative(dir, i);
            BlockState state = level.getBlockState(probe);
            IBlowable blowable = NtmContracts.BLOWABLE.at(level, probe, state);

            if ((state.isCollisionShapeFullBlock(level, probe)
                            && state.canOcclude()
                            && !state.isSignalSource())
                    || blowable != null) {
                if (!level.isClientSide() && blowable != null) {
                    blowable.applyFan(level, probe, dir, i);
                }

                break;
            }

            effRange = i;
        }

        int x = dir.getStepX() * effRange;
        int y = dir.getStepY() * effRange;
        int z = dir.getStepZ() * effRange;

        List<Entity> affected =
                level.getEntitiesOfClass(
                        Entity.class,
                        new AABB(
                                        worldPosition.getX() + 0.5 + Math.min(x, 0),
                                        worldPosition.getY() + 0.5 + Math.min(y, 0),
                                        worldPosition.getZ() + 0.5 + Math.min(z, 0),
                                        worldPosition.getX() + 0.5 + Math.max(x, 0),
                                        worldPosition.getY() + 0.5 + Math.max(y, 0),
                                        worldPosition.getZ() + 0.5 + Math.max(z, 0))
                                .inflate(0.5, 0.5, 0.5));

        for (Entity e : affected) {

            double coeff = push;

            if (falloff) {
                double dist =
                        Math.sqrt(
                                e.distanceToSqr(
                                        worldPosition.getX() + 0.5,
                                        worldPosition.getY() + 0.5,
                                        worldPosition.getZ() + 0.5));
                coeff *= 1.5 * (1 - dist / range / 2);
            }
            if (suck) coeff *= -1;

            e.setDeltaMovement(
                    e.getDeltaMovement()
                            .add(
                                    dir.getStepX() * coeff,
                                    dir.getStepY() * coeff,
                                    dir.getStepZ() * coeff));
        }

        if (level.isClientSide() && level.getRandom().nextInt(30) == 0) {
            double speed = suck ? -0.2 : 0.2;
            level.addParticle(
                    ParticleTypes.CLOUD,
                    worldPosition.getX() + 0.5 + dir.getStepX() * 0.5,
                    worldPosition.getY() + 0.5 + dir.getStepY() * 0.5,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.5,
                    dir.getStepX() * speed,
                    dir.getStepY() * speed,
                    dir.getStepZ() * speed);
        }

        this.spin += 30;

        if (this.spin >= 360) {
            this.prevSpin -= 360;
            this.spin -= 360;
        }

        if (!level.isClientSide()) {
            networkPackNT(150);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        falloff = input.getBooleanOr("falloff", falloff);
        suck = input.getBooleanOr("suck", suck);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("falloff", falloff);
        output.putBoolean("suck", suck);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.falloff);
            case 1 -> output.writeBoolean(this.suck);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.falloff = input.readBoolean();
            case 1 -> this.suck = input.readBoolean();
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
