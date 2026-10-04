// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.Floodlight;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.util.ChunkUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.backport.BlockEntityCompat;
import com.hbm.backport.SubLevelSpace;

public class BlockEntityFloodlight extends BlockEntityCompat
        implements IEnergyHandlerMK2, Synced, GraphResident, SyncUnitSchema {

    public static final long MAX_POWER = 5_000L;
    private static final long POWER_DRAW = 100L;
    private static final int BEAM_COUNT = 15;
    private static final int MAX_BEAM_DISTANCE = 64;
    private static final int LIGHT_DAMPENING_CUTOFF = 8;
    private final BlockPos[] lightPositions = new BlockPos[BEAM_COUNT];

    @SyncField(units = 1L << 0)
    public float rotation;

    @SyncField(units = 1L << 1)
    public long power;

    @SyncField(units = 1L << 2)
    public boolean isOn;

    private int delay;

    public BlockEntityFloodlight(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOODLIGHT.get(), pos, state);
    }

    private static float[] getVariation(int index) {
        return new float[] {
            (((index / 3) - 2) * 7.5F) / 180F * (float) Math.PI,
            (((index % 3) - 1) * 15F) / 180F * (float) Math.PI
        };
    }

    public void tickServer() {
        if (delay > 0) {
            delay--;
            return;
        }

        if (power >= POWER_DRAW) {
            power -= POWER_DRAW;
            if (!isOn) {
                isOn = true;
                castLights();
                syncVisualState();
            } else if (TickPhase.every(this, 5)) {
                // backport-fix: BF-042 on a build the ground under the beams changes as it moves
                if (onSubLevel()) {
                    castLights();
                } else {
                    long timer = level.getGameTime() / 5L;
                    castLight((int) Math.abs(timer % lightPositions.length));
                }
            }
        } else if (isOn) {
            isOn = false;
            delay = 60;
            destroyLights();
            syncVisualState();
        }
    }

    public Direction inputDirection() {
        return Direction.from3DDataValue(getBlockState().getValue(Floodlight.FACING) % 6)
                .getOpposite();
    }

    private void castLight(int index) {
        BlockPos newPos = getRayEndpoint(index);
        BlockPos oldPos = lightPositions[index];
        lightPositions[index] = null;

        if (newPos == null || !newPos.equals(oldPos)) {
            if (oldPos != null
                    && ChunkUtil.blockEntityIfLoaded(level, oldPos)
                            instanceof BlockEntityFloodlightBeam beam
                    && beam.isFrom(worldPosition)) {
                level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        if (newPos == null || ChunkUtil.chunkIfLoaded(level, newPos) == null) return;

        if (level.getBlockState(newPos).isAir()) {
            level.setBlock(
                    newPos,
                    ModBlocks.FLOODLIGHT_BEAM.get().defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(newPos) instanceof BlockEntityFloodlightBeam beam) {
                beam.setSource(this, index);
            }
            lightPositions[index] = newPos;
        }

        if (level.getBlockState(newPos).is(ModBlocks.FLOODLIGHT_BEAM.get())) {
            lightPositions[index] = newPos;
        }
    }

    private @Nullable BlockPos getRayEndpoint(int index) {
        if (index < 0 || index >= lightPositions.length) return null;

        int meta = getBlockState().getValue(Floodlight.FACING);
        float[] angles = getVariation(index);
        float adjustedRotation = rotation;
        if (meta == 1 || meta == 7) adjustedRotation = 180F - adjustedRotation;
        if (meta == 6) adjustedRotation = 180F - adjustedRotation;

        Vec3 direction =
                new Vec3(1D, 0D, 0D).zRot((float) (adjustedRotation / 180D * Math.PI) + angles[0]);
        if (meta == 6 || meta == 7 || meta == 2) direction = direction.yRot((float) (Math.PI / 2D));
        if (meta == 3) direction = direction.yRot((float) -(Math.PI / 2D));
        if (meta == 4) direction = direction.yRot((float) Math.PI);
        direction = direction.yRot(angles[1]);

        if (onSubLevel()) return getRayEndpointSubLevel(direction);

        for (int distance = 1; distance < MAX_BEAM_DISTANCE; distance++) {
            BlockPos tested =
                    BlockPos.containing(
                            worldPosition.getX() + 0.5D + direction.x * distance,
                            worldPosition.getY() + 0.5D + direction.y * distance,
                            worldPosition.getZ() + 0.5D + direction.z * distance);
            if (tested.equals(worldPosition)) continue;
            BlockState blocking = ChunkUtil.blockStateIfLoaded(level, tested);
            if (blocking == null) return null;

            if (blocking.getLightBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) < LIGHT_DAMPENING_CUTOFF) continue;

            if (distance > 1) {
                return BlockPos.containing(
                        worldPosition.getX() + 0.5D + direction.x * (distance - 1),
                        worldPosition.getY() + 0.5D + direction.y * (distance - 1),
                        worldPosition.getZ() + 0.5D + direction.z * (distance - 1));
            }
        }

        return null;
    }

    private boolean onSubLevel() {
        return SubLevelSpace.inSubLevel(
                level, worldPosition.getX() + 0.5D, worldPosition.getZ() + 0.5D);
    }

    private static boolean blocksLight(BlockState state) {
        return state.getLightBlock(
                        net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                        net.minecraft.core.BlockPos.ZERO)
                >= LIGHT_DAMPENING_CUTOFF;
    }

    /**
     * backport-fix: BF-042 — a floodlight on a physics build marches its ray through the build
     * (its own plot) and through the world at the build's pose side by side. Whichever it hits
     * first gets the light: the build's own surfaces, or the world outside it. Before, the ray only
     * saw the build and empty plot air, so the light never left the build.
     */
    private @Nullable BlockPos getRayEndpointSubLevel(Vec3 direction) {
        Vec3 start =
                new Vec3(
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D);
        Vec3 worldStart = SubLevelSpace.toWorld(level, start.x, start.y, start.z);
        Vec3 worldDirection =
                SubLevelSpace.toWorld(
                                level,
                                start.x + direction.x,
                                start.y + direction.y,
                                start.z + direction.z)
                        .subtract(worldStart);

        for (int distance = 1; distance < MAX_BEAM_DISTANCE; distance++) {
            BlockPos own = BlockPos.containing(start.add(direction.scale(distance)));
            if (!own.equals(worldPosition)) {
                BlockState blocking = ChunkUtil.blockStateIfLoaded(level, own);
                if (blocking != null && blocksLight(blocking)) {
                    return distance > 1
                            ? BlockPos.containing(start.add(direction.scale(distance - 1)))
                            : null;
                }
            }

            BlockPos ground = BlockPos.containing(worldStart.add(worldDirection.scale(distance)));
            BlockState blocking = ChunkUtil.blockStateIfLoaded(level, ground);
            if (blocking == null) return null;
            if (blocksLight(blocking)) {
                return distance > 1
                        ? BlockPos.containing(worldStart.add(worldDirection.scale(distance - 1)))
                        : null;
            }
        }

        return null;
    }

    private void castLights() {
        for (int index = 0; index < lightPositions.length; index++) castLight(index);
    }

    private void destroyLight(int index) {
        BlockPos pos = lightPositions[index];
        BlockState beam = pos == null ? null : ChunkUtil.blockStateIfLoaded(level, pos);
        if (beam != null && beam.is(ModBlocks.FLOODLIGHT_BEAM.get())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void destroyLights() {
        for (int index = 0; index < lightPositions.length; index++) destroyLight(index);
    }

    boolean ownsBeam(BlockPos pos, int index) {
        return pos.equals(lightPositions[index]);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) destroyLights();
    }

    private void syncVisualState() {
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putFloat("rotation", rotation);
        out.putLong("power", power);
        out.putBoolean("isOn", isOn);
        int cast = 0;
        List<BlockPos> lights = new ArrayList<>();
        for (int i = 0; i < lightPositions.length; i++) {
            if (lightPositions[i] == null) continue;
            cast |= 1 << i;
            lights.add(lightPositions[i]);
        }
        out.putInt("lightMask", cast);
        out.store("lights", BlockPos.CODEC.listOf(), lights);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        rotation = in.getFloatOr("rotation", 0F);
        power = in.getLongOr("power", 0L);
        isOn = in.getBooleanOr("isOn", false);
        int cast = in.getIntOr("lightMask", 0);
        List<BlockPos> lights = in.read("lights", BlockPos.CODEC.listOf()).orElse(List.of());
        for (int i = 0, next = 0; i < lightPositions.length; i++) {
            lightPositions[i] =
                    (cast & 1 << i) != 0 && next < lights.size() ? lights.get(next++) : null;
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeFloat(this.rotation);
            case 1 -> output.writeLong(this.power);
            case 2 -> output.writeBoolean(this.isOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.rotation = input.readFloat();
            case 1 -> this.power = input.readLong();
            case 2 -> this.isOn = input.readBoolean();
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
}
