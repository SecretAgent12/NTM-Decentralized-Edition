// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.BlockDustBurstPayload;
import com.hbm.platform.Services;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.Synced;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import com.hbm.backport.BlockEntityCompat;

public class BlockEntityMachineAutosaw extends BlockEntityCompat
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                AudioLoop,
                FluidTankEndpoint,
                IFluidCopiable,
                SyncUnitSchema {

    public static final double AUDIO_RANGE_SQ = 15 * 15;
    private static final int MIN_DIST = 2;
    private static final int MAX_DIST = 9;
    private static final int FELL_HORIZONTAL_RANGE = 10;
    private static final int FELL_BFS_RADIUS = MAX_DIST + FELL_HORIZONTAL_RANGE;
    private static final int FELL_VERTICAL_RANGE = 32;
    private static final int FELL_MAX_BASE_DEPTH = FELL_VERTICAL_RANGE / 2;

    private static final int[][] EIGHTEEN_DIRS = {
        {1, 0, 0},
        {-1, 0, 0},
        {0, 1, 0},
        {0, -1, 0},
        {0, 0, 1},
        {0, 0, -1},
        {1, 1, 0},
        {1, -1, 0},
        {-1, 1, 0},
        {-1, -1, 0},
        {1, 0, 1},
        {1, 0, -1},
        {-1, 0, 1},
        {-1, 0, -1},
        {0, 1, 1},
        {0, 1, -1},
        {0, -1, 1},
        {0, -1, -1}
    };
    public static @Nullable Consumer<BlockEntityMachineAutosaw> CLIENT_SOUND;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.WOODOIL, 100);

    private final FluidTankNTM[] receiving;

    @SyncField(units = 1L << 0)
    public boolean isOn;

    @SyncField(units = 1L << 1)
    public boolean isSuspended;

    public float syncYaw;

    @SyncField(units = 1L << 2)
    public float rotationYaw;

    public float prevRotationYaw;
    public float syncPitch;

    @SyncField(units = 1L << 3)
    public float rotationPitch;

    public float prevRotationPitch;
    public float spin, lastSpin;
    private int forceSkip;
    private int state;
    private int turnProgress;

    public BlockEntityMachineAutosaw(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOSAW.get(), pos, state);
        receiving = new FluidTankNTM[] {tank};
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityMachineAutosaw be) {
        be.tickServer((ServerLevel) level);
    }

    private static boolean shouldIgnore(BlockState state) {
        return (state.is(ModBlocks.PLANT_TALL_CD2.get())
                        || state.is(ModBlocks.PLANT_TALL_CD3.get()))
                && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    private static boolean canSupportSapling(ServerLevel level, int x, int y, int z) {
        return Blocks.OAK_SAPLING.defaultBlockState().canSurvive(level, new BlockPos(x, y + 1, z));
    }

    private static Block saplingFor(Block log) {
        if (log == Blocks.SPRUCE_LOG) return Blocks.SPRUCE_SAPLING;
        if (log == Blocks.BIRCH_LOG) return Blocks.BIRCH_SAPLING;
        if (log == Blocks.JUNGLE_LOG) return Blocks.JUNGLE_SAPLING;
        if (log == Blocks.ACACIA_LOG) return Blocks.ACACIA_SAPLING;
        if (log == Blocks.DARK_OAK_LOG) return Blocks.DARK_OAK_SAPLING;
        // backport: no pale oak in 1.21.1
        return Blocks.OAK_SAPLING;
    }

    private static boolean isWood(BlockState s) {
        return s.is(BlockTags.LOGS);
    }

    private static boolean isLeaves(BlockState s) {
        return s.is(BlockTags.LEAVES);
    }

    private static boolean isPlant(BlockState s) {
        return s.getBlock() instanceof BushBlock;
    }

    public void toggleSuspended() {
        isSuspended = !isSuspended;
        setChanged();
    }

    private void tickServer(ServerLevel level) {
        if (!isSuspended && TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            if (tank.getFill() > 0) {
                tank.setFill(tank.getFill() - 1);
                isOn = true;
            } else isOn = false;
        }

        if (isOn && !isSuspended) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            Vec3 pivot = new Vec3(x + 0.5, y + 1.75, z + 0.5);
            Vec3 upperArm =
                    new Vec3(0, 0, -4)
                            .xRot((float) Math.toRadians(80 - rotationPitch))
                            .yRot(-(float) Math.toRadians(rotationYaw));
            Vec3 lowerArm =
                    new Vec3(0, 0, -4)
                            .xRot((float) -Math.toRadians(80 - rotationPitch))
                            .yRot(-(float) Math.toRadians(rotationYaw));
            Vec3 armTip = new Vec3(0, 0, -2).yRot(-(float) Math.toRadians(rotationYaw));
            double cX = pivot.x + upperArm.x + lowerArm.x + armTip.x;
            double cY = pivot.y;
            double cZ = pivot.z + upperArm.z + lowerArm.z + armTip.z;

            for (LivingEntity e :
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            new AABB(cX - 1, cY - 0.25, cZ - 1, cX + 1, cY + 0.25, cZ + 1))) {
                if (e.isAlive()
                        && e.hurt(level.damageSources().source(ModDamageTypes.BLENDER), 100)) {
                    level.playSound(
                            null,
                            e.getX(),
                            e.getY(),
                            e.getZ(),
                            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.BLOCKS,
                            2.0F,
                            0.95F + level.getRandom().nextFloat() * 0.2F);
                    int count = Math.min((int) Math.ceil(e.getMaxHealth() / 4), 250) * 4;
                    Services.NETWORK.sendToAllAround(
                            new BlockDustBurstPayload(
                                    Blocks.REDSTONE_BLOCK.defaultBlockState(),
                                    e.getX(),
                                    e.getY() + e.getBbHeight() * 0.5,
                                    e.getZ(),
                                    count,
                                    0.1D),
                            new TargetPoint(level, e.getX(), e.getY(), e.getZ(), 50));
                }
            }

            if (state == 0) {
                rotationYaw += 1;
                if (rotationYaw >= 360) rotationYaw -= 360;

                if (forceSkip > 0) {
                    forceSkip--;
                } else {
                    final double CUT_ANGLE = 0.08726646259971647;
                    double rotationYawRads = Math.toRadians((rotationYaw + 270) % 360);
                    outer:
                    for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
                        for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                            int sqrDst = dx * dx + dz * dz;
                            if (sqrDst <= MIN_DIST * MIN_DIST || sqrDst > MAX_DIST * MAX_DIST)
                                continue;
                            double angle = Math.atan2(dz, dx);
                            double relAngle = Math.abs(angle - rotationYawRads);
                            relAngle = Math.abs((relAngle + Math.PI) % (2 * Math.PI) - Math.PI);
                            if (relAngle > CUT_ANGLE) continue;
                            BlockPos p = new BlockPos(x + dx, y + 1, z + dz);
                            BlockState bs = level.getBlockState(p);
                            if (!(isWood(bs) || isLeaves(bs) || isPlant(bs))) continue;
                            if (shouldIgnore(bs)) continue;
                            state = 1;
                            break outer;
                        }
                    }
                }
            }

            int hitY = Mth.floor(cY);
            int hitX0 = Mth.floor(cX - 0.5), hitZ0 = Mth.floor(cZ - 0.5);
            int hitX1 = Mth.floor(cX + 0.5), hitZ1 = Mth.floor(cZ + 0.5);
            tryInteract(level, hitX0, hitY, hitZ0);
            tryInteract(level, hitX1, hitY, hitZ0);
            tryInteract(level, hitX0, hitY, hitZ1);
            tryInteract(level, hitX1, hitY, hitZ1);

            if (state == 1) {
                rotationPitch += 2;
                if (rotationPitch > 80) {
                    rotationPitch = 80;
                    state = 2;
                }
            }
            if (state == 2) {
                rotationPitch -= 2;
                if (rotationPitch <= 0) {
                    rotationPitch = 0;
                    state = 0;
                }
            }
        }

        networkPackNT(100);
    }

    private void tryInteract(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState bs = level.getBlockState(pos);
        if (!shouldIgnore(bs)) {
            if (isLeaves(bs) || isPlant(bs)) {
                cutCrop(level, pos, bs);
            } else if (isWood(bs)) {
                fellTree(level, x, y, z);
                if (state == 1) state = 2;
            }
        }

        BlockState ahead = level.getBlockState(pos);
        if (state == 1 && ahead.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) && !ahead.isSignalSource()) {
            state = 2;
            forceSkip = 5;
        }
    }

    private void cutCrop(ServerLevel level, BlockPos pos, BlockState bs) {
        level.levelEvent(2001, pos, Block.getId(bs));
        for (ItemStack drop : Block.getDrops(bs, level, pos, null)) {
            float delta = 0.7F;
            double dx = level.getRandom().nextFloat() * delta + (1.0F - delta) * 0.5D;
            double dy = level.getRandom().nextFloat() * delta + (1.0F - delta) * 0.5D;
            double dz = level.getRandom().nextFloat() * delta + (1.0F - delta) * 0.5D;
            ItemEntity item =
                    new ItemEntity(level, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, drop);
            item.setPickUpDelay(10);
            level.addFreshEntity(item);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void fellTree(ServerLevel level, int hitX, int hitY, int hitZ) {
        int cx = worldPosition.getX(), cz = worldPosition.getZ();
        int sawY = hitY;
        BlockPos hitCol = new BlockPos(hitX, -1, hitZ);

        HashMap<BlockPos, BlockPos> trunks = new HashMap<>();
        for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
            for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                if (dx * dx + dz * dz > MAX_DIST * MAX_DIST) continue;
                int colX = cx + dx, colZ = cz + dz;
                if (!isWood(level.getBlockState(new BlockPos(colX, sawY, colZ)))) continue;
                int baseY = sawY;
                while (sawY - baseY < FELL_MAX_BASE_DEPTH
                        && isWood(level.getBlockState(new BlockPos(colX, baseY - 1, colZ))))
                    baseY--;
                if (!canSupportSapling(level, colX, baseY - 1, colZ)) continue;
                trunks.put(new BlockPos(colX, -1, colZ), new BlockPos(colX, baseY, colZ));
            }
        }
        if (!trunks.containsKey(hitCol)) {
            int baseY = hitY;
            while (sawY - baseY < FELL_MAX_BASE_DEPTH
                    && isWood(level.getBlockState(new BlockPos(hitX, baseY - 1, hitZ)))) baseY--;
            trunks.put(hitCol, new BlockPos(hitX, baseY, hitZ));
        }

        HashMap<BlockPos, BlockPos> blockOwner = new HashMap<>();
        ArrayDeque<BlockPos[]> deque = new ArrayDeque<>();
        int[] hitColCount = {1};
        int minY = Math.max(0, sawY - FELL_MAX_BASE_DEPTH);
        int maxY = Math.min(255, sawY + FELL_VERTICAL_RANGE);

        for (Map.Entry<BlockPos, BlockPos> trunk : trunks.entrySet()) {
            deque.addFirst(new BlockPos[] {trunk.getValue(), trunk.getKey()});
        }

        while (!deque.isEmpty()) {
            BlockPos[] pair = deque.pollFirst();
            BlockPos current = pair[0];
            BlockPos currentCol = pair[1];

            if (blockOwner.containsKey(current)) {
                if (currentCol.equals(hitCol) && --hitColCount[0] == 0) break;
                continue;
            }
            blockOwner.put(current, currentCol);

            for (int[] dir : EIGHTEEN_DIRS) {
                int nx = current.getX() + dir[0],
                        ny = current.getY() + dir[1],
                        nz = current.getZ() + dir[2];
                int ndx = nx - cx, ndz = nz - cz;
                if (ndx * ndx + ndz * ndz > FELL_BFS_RADIUS * FELL_BFS_RADIUS) continue;
                if (ny < minY || ny > maxY) continue;
                BlockPos np = new BlockPos(nx, ny, nz);
                if (blockOwner.containsKey(np)) continue;
                BlockState bs = level.getBlockState(np);
                if (!isWood(bs) && !isLeaves(bs)) continue;
                boolean hasHorizontal = dir[0] != 0 || dir[2] != 0;
                BlockPos[] entry = new BlockPos[] {np, currentCol};
                if (!hasHorizontal) deque.addFirst(entry);
                else deque.addLast(entry);
                if (currentCol.equals(hitCol)) hitColCount[0]++;
            }

            if (currentCol.equals(hitCol) && --hitColCount[0] == 0) break;
        }

        for (Map.Entry<BlockPos, BlockPos> entry : blockOwner.entrySet()) {
            if (!entry.getValue().equals(hitCol)) continue;
            BlockPos pos = entry.getKey();
            BlockState bs = level.getBlockState(pos);
            if (isWood(bs)
                    && isWithinWorkingArea(pos.getX(), pos.getZ())
                    && canSupportSapling(level, pos.getX(), pos.getY() - 1, pos.getZ())) {
                level.destroyBlock(pos, true);
                level.setBlock(pos, saplingFor(bs.getBlock()).defaultBlockState(), 3);
            } else {
                level.destroyBlock(pos, true);
            }
        }
    }

    private boolean isWithinWorkingArea(int x, int z) {
        int dx = x - worldPosition.getX(), dz = z - worldPosition.getZ();
        int distSq = dx * dx + dz * dz;
        return distSq > MIN_DIST * MIN_DIST && distSq <= MAX_DIST * MAX_DIST;
    }

    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);

        lastSpin = spin;
        if (isOn && !isSuspended) {
            spin += 15F;
            Vec3 vec = new Vec3(0.625, 0, 1.625).yRot(-(float) Math.toRadians(rotationYaw));
            level.addParticle(
                    ParticleTypes.SMOKE,
                    worldPosition.getX() + 0.5 + vec.x,
                    worldPosition.getY() + 2.0625,
                    worldPosition.getZ() + 0.5 + vec.z,
                    0,
                    0,
                    0);
        }
        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        if (turnProgress > 0) {
            rotationYaw =
                    (float)
                            (rotationYaw
                                    + Mth.wrapDegrees(syncYaw - (double) rotationYaw)
                                            / turnProgress);
            rotationPitch =
                    (float)
                            (rotationPitch
                                    + Mth.wrapDegrees(syncPitch - (double) rotationPitch)
                                            / turnProgress);
            turnProgress--;
        } else {
            rotationYaw = syncYaw;
            rotationPitch = syncPitch;
        }

        if (rotationYaw >= 360F) {
            rotationYaw -= 360F;
            prevRotationYaw -= 360F;
        } else if (rotationYaw < 0F) {
            rotationYaw += 360F;
            prevRotationYaw += 360F;
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ENGINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                10F,
                1.0F + level.getRandom().nextFloat() * 0.1F,
                10);
    }

    public boolean acceptsFace(Direction dir) {
        return dir != Direction.UP;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    private void readYaw(ByteBuf input) {
        syncYaw = input.readFloat();
        turnProgress = 3;
    }

    private void readPitch(ByteBuf input) {
        syncPitch = input.readFloat();
        turnProgress = 3;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isOn = input.getBooleanOr("isOn", isOn);
        isSuspended = input.getBooleanOr("isSuspended", isSuspended);
        forceSkip = input.getIntOr("skip", forceSkip);
        rotationYaw = input.getFloatOr("yaw", rotationYaw);
        rotationPitch = input.getFloatOr("pitch", rotationPitch);
        state = input.getIntOr("state", state);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("isSuspended", isSuspended);
        output.putInt("skip", forceSkip);
        output.putFloat("yaw", rotationYaw);
        output.putFloat("pitch", rotationPitch);
        output.putInt("state", state);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isOn);
            case 1 -> output.writeBoolean(this.isSuspended);
            case 2 -> output.writeFloat(this.rotationYaw);
            case 3 -> output.writeFloat(this.rotationPitch);
            case 4 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.isOn = input.readBoolean();
            case 1 -> this.isSuspended = input.readBoolean();
            case 2 -> readYaw(input);
            case 3 -> readPitch(input);
            case 4 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }


    // backport: woven trait AudioLoop
    private AudioWrapper hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
            // backport: woven trait Synced
        hbm$syncRemove();
        com.hbm.tileentity.FoldedCoreResident.onRemove(this); // backport: hbm-compiler FoldedLifecycle
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
