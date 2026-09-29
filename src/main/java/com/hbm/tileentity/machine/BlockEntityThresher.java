// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.blocks.machine.MachineThresher;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
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
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
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

public class BlockEntityThresher extends BlockEntityCompat
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                AudioLoop,
                FluidTankEndpoint,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int TANK_CAPACITY = 100;
    public static final float SWING_MAX_ANGLE = 82.5F;
    public static final float SWING_SPEED = 82.5F / 60F;
    public static final int IDLE_DELAY_BASE = 200, IDLE_DELAY_RAND = 100;
    public static final int SCAN_RADIUS = 3;
    public static final float ENTITY_DAMAGE = 100F;
    public static final double AUDIO_RANGE_SQ = 15 * 15;
    public static @Nullable Consumer<BlockEntityThresher> CLIENT_SOUND;

    @SyncField(units = 1L << 3)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.WOODOIL, TANK_CAPACITY);

    private final FluidTankNTM[] receiving;

    @SyncField(units = 1L << 0)
    public boolean isOn;

    @SyncField(units = 1L << 1)
    public boolean isSuspended;

    public float syncAngle;

    @SyncField(units = 1L << 2)
    public float angle;

    public float prevAngle;
    public float spin;
    public float lastSpin;
    private int delay;
    private int state;
    private int turnProgress;

    public BlockEntityThresher(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THRESHER.get(), pos, state);
        receiving = new FluidTankNTM[] {tank};
    }

    public static boolean acceptsFuel(Fluid type) {
        return type == NTMFluids.WOODOIL
                || type == NTMFluids.ETHANOL
                || type == NTMFluids.FISHOIL
                || type == NTMFluids.HEAVYOIL
                || type == NTMFluids.COALCREOSOTE;
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityThresher be) {
        be.tickServer((ServerLevel) level);
    }

    private Direction rearDirection() {
        return getBlockState().getValue(MachineThresher.FACING).getOpposite();
    }

    public boolean acceptsFuelFrom(@Nullable Direction side) {
        if (side == null) return true;
        Direction dir = rearDirection();
        Direction rot = dir.getClockWise();
        return side == rot || side == rot.getOpposite() || side == Direction.DOWN;
    }

    private void tickServer(ServerLevel level) {
        Direction dir = rearDirection();
        Direction rot = dir.getClockWise();

        if (!isSuspended && TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            if (tank.getFill() > 0) {
                tank.setFill(tank.getFill() - 1);
                isOn = true;
            } else {
                isOn = false;
            }
        }

        if (isOn && !isSuspended) {
            if (state == 0) {
                delay--;
                if (delay <= 0) state = 1;
            }
            if (state == 1) {
                angle += SWING_SPEED;
                if (angle >= SWING_MAX_ANGLE) {
                    angle = SWING_MAX_ANGLE;
                    state = 2;
                }
            } else if (state == 2) {
                angle -= SWING_SPEED;
                if (angle <= 0F) {
                    angle = 0F;
                    state = 0;
                    delay = IDLE_DELAY_BASE + level.getRandom().nextInt(IDLE_DELAY_RAND);
                }
            }

            if (angle != 0F) swing(level, dir, rot);
        }

        networkPackNT(100);
    }

    private void swing(ServerLevel level, Direction dir, Direction rot) {
        Vec3 pivot =
                new Vec3(
                        worldPosition.getX() + 0.5 - dir.getStepX(),
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5 - dir.getStepZ());
        Vec3 upperArm = new Vec3(-dir.getStepX() * 4, 0, -dir.getStepZ() * 4);
        Vec3 lowerArm = new Vec3(-dir.getStepX() * 4, 0, -dir.getStepZ() * 4);
        float rad = (float) Math.toRadians(SWING_MAX_ANGLE - angle);
        if (dir.getStepZ() != 0) {
            upperArm = upperArm.xRot(rad);
            lowerArm = lowerArm.xRot(-rad);
        }
        if (dir.getStepX() != 0) {
            upperArm = upperArm.zRot(rad);
            lowerArm = lowerArm.zRot(-rad);
        }
        Vec3 armTip = new Vec3(-dir.getStepX() * 2, 0, -dir.getStepZ() * 2);

        double endX = pivot.x + upperArm.x + lowerArm.x + armTip.x;
        double endZ = pivot.z + upperArm.z + lowerArm.z + armTip.z;
        int y = worldPosition.getY();

        for (int i = -SCAN_RADIUS; i <= SCAN_RADIUS; i++) {
            int hitX = Mth.floor(endX + rot.getStepX() * i);
            int hitZ = Mth.floor(endZ + rot.getStepZ() * i);
            BlockPos hit = new BlockPos(hitX, y, hitZ);
            BlockState bs = level.getBlockState(hit);
            Block b = bs.getBlock();

            if (bs.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) && !bs.isSignalSource() && !canCut(b)) {
                state = 2;
                break;
            }

            if (b == Blocks.SUNFLOWER) {
                if (level.getRandom().nextInt(250) == 0) {
                    spawnBreakFx(level, hit, bs);
                    dropItem(level, new ItemStack(Blocks.SUNFLOWER));
                }
                continue;
            }
            if (b == Blocks.TALL_GRASS) {
                if (level.getRandom().nextInt(100) == 0) {
                    spawnBreakFx(level, hit, bs);
                    dropItem(level, new ItemStack(Items.WHEAT_SEEDS));
                }
                continue;
            }
            if (b instanceof BlockTallPlant) {
                cutTallPlant(level, b, hit, bs);
                continue;
            }

            if (b == Blocks.SUGAR_CANE || b == Blocks.CACTUS) {
                cutCane(level, b, hitX, y, hitZ);
                continue;
            }

            if (canCut(b) && !shouldIgnore(level, hit, bs)) cutCrop(level, hit, bs);
        }

        AABB box =
                new AABB(endX, y + 0.5, endZ, endX, y + 0.5, endZ)
                        .inflate(
                                Math.abs(dir.getStepX() * 0.5) + Math.abs(rot.getStepX() * 4.5),
                                0.5,
                                Math.abs(dir.getStepZ() * 0.5) + Math.abs(rot.getStepZ() * 4.5));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e.isAlive()
                    && e.hurt(level.damageSources().source(ModDamageTypes.BLENDER), ENTITY_DAMAGE)) {
                if (e instanceof Enemy && !e.isAlive())
                    dropItem(level, new ItemStack(ModItems.NITRA_SMALL));
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
    }

    private static boolean canCut(Block block) {
        return block instanceof BonemealableBlock
                || block == Blocks.NETHER_WART
                || block == Blocks.MELON
                || block == Blocks.PUMPKIN;
    }

    private static boolean shouldIgnore(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof StemBlock) return true;
        if (block == Blocks.NETHER_WART)
            return state.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE;
        return block instanceof BonemealableBlock crop
                && crop.isValidBonemealTarget(level, pos, state);
    }

    private void spawnBreakFx(ServerLevel level, BlockPos pos, BlockState state) {
        level.levelEvent(2001, pos, Block.getId(state));
    }

    private void cutTallPlant(ServerLevel level, Block plant, BlockPos pos, BlockState bs) {
        if (bs.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
            pos = pos.above();
            bs = level.getBlockState(pos);
            if (!bs.is(plant)) return;
        }
        if (plant == ModBlocks.PLANT_TALL_CD2.get() || plant == ModBlocks.PLANT_TALL_CD3.get())
            return;

        spawnBreakFx(level, pos, bs);
        for (ItemStack drop : Block.getDrops(bs, level, pos, null)) dropItem(level, drop);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void cutCane(ServerLevel level, Block target, int x, int y, int z) {
        int offset = level.getBlockState(new BlockPos(x, y - 1, z)).is(target) ? -1 : 0;
        for (int i = 2 + offset; i > offset; i--) {
            BlockPos pos = new BlockPos(x, y + i, z);
            BlockState bs = level.getBlockState(pos);
            spawnBreakFx(level, pos, bs);
            for (ItemStack drop : Block.getDrops(bs, level, pos, null)) dropItem(level, drop);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void cutCrop(ServerLevel level, BlockPos pos, BlockState bs) {
        spawnBreakFx(level, pos, bs);

        BlockState replacement = Blocks.AIR.defaultBlockState();
        boolean replanted = false;

        for (ItemStack drop : Block.getDrops(bs, level, pos, null)) {
            if (!replanted
                    && drop.getItem() instanceof BlockItem seed
                    && seed.getBlock() instanceof BushBlock plant) {
                BlockState planted = plant.defaultBlockState();

                if (planted.canSurvive(level, pos)) {
                    replacement = planted;
                    replanted = true;
                    drop.shrink(1);
                    if (drop.isEmpty()) continue;
                }
            }
            dropItem(level, drop);
        }

        if (bs.getBlock() == Blocks.WHEAT && !replanted)
            replacement = Blocks.WHEAT.defaultBlockState();

        level.setBlock(pos, replacement, 3);
    }

    private void dropItem(ServerLevel level, ItemStack drop) {
        Direction dir = getBlockState().getValue(MachineThresher.FACING);
        double x = worldPosition.getX() + 0.5 - dir.getStepX() * 0.75;
        double z = worldPosition.getZ() + 0.5 - dir.getStepZ() * 0.75;
        ItemEntity item = new ItemEntity(level, x, worldPosition.getY(), z, drop);
        item.setPickUpDelay(10);
        item.setDeltaMovement(
                dir.getStepX() * -0.2 + 0.2, item.getDeltaMovement().y, dir.getStepZ() * -0.2);
        level.addFreshEntity(item);
    }

    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);

        lastSpin = spin;
        if (isOn && !isSuspended) {
            if (angle > 0) spin += 15F;
            Direction dir = rearDirection();
            Direction rot = dir.getClockWise();
            level.addParticle(
                    ParticleTypes.SMOKE,
                    worldPosition.getX() + 0.5 + dir.getStepX() * 0.8125 + rot.getStepX() * 0.375,
                    worldPosition.getY() + 1.5625,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.8125 + rot.getStepZ() * 0.375,
                    0,
                    0,
                    0);
        }
        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        prevAngle = angle;
        if (turnProgress > 0) {
            double d0 = Mth.wrapDegrees(syncAngle - (double) angle);
            angle = (float) (angle + d0 / turnProgress);
            turnProgress--;
        } else {
            angle = syncAngle;
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

    public void toggleSuspended() {
        isSuspended = !isSuspended;
        setChanged();
    }

    public boolean acceptsFace(Direction dir) {
        return acceptsFuelFrom(dir);
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isOn = input.getBooleanOr("isOn", isOn);
        isSuspended = input.getBooleanOr("isSuspended", isSuspended);
        angle = input.getFloatOr("angle", angle);
        state = input.getIntOr("state", state);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("isSuspended", isSuspended);
        output.putFloat("angle", angle);
        output.putInt("state", state);
        tank.serialize(output.child("tank"));
    }

    private void readAngle(ByteBuf input) {
        syncAngle = input.readFloat();
    }

    @Override
    public void afterSyncUnits(long units) {
        turnProgress = 3;
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isOn);
            case 1 -> output.writeBoolean(this.isSuspended);
            case 2 -> output.writeFloat(this.angle);
            case 3 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.isOn = input.readBoolean();
            case 1 -> this.isSuspended = input.readBoolean();
            case 2 -> readAngle(input);
            case 3 -> this.tank.packetDeserialize(input);
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
