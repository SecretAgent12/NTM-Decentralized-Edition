// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.BlockEntityMachinePolluting;
import com.hbm.blockentity.IFluidCopiable;
import com.hbm.blockentity.IUpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.MenuMachineTurbofan;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.GasFlamePayload;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.TickPhase;
import dev.ryanhcode.sable.companion.SableCompanion;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityMachineTurbofan extends BlockEntityMachinePolluting
        implements FluidTankAccess,
                AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_CONTAINER_OUT = 1;
    public static final int SLOT_UPGRADE = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_FLUID_ID = 4;
    public static final int SLOT_COUNT = 5;

    public static final long MAX_POWER = 1_000_000L;
    public static final int FLUID_CAP = 24_000;
    public static final int SMOKE_CAP = 150;

    public static final int PONY_AFTERBURNER = 100;

    // backport: control modes (not in 1.7.10 / ntm-next), switched with a screwdriver
    /** As in 1.7.10: runs at 0, any redstone signal stops it. */
    public static final int MODE_CLASSIC = 0;
    /** Throttle, the way Create machines take redstone: 0 stops, 1-15 run at signal/15 of full. */
    public static final int MODE_THROTTLE = 1;

    /** Thrust (Sable mass units x m/s^2) at full throttle without afterburner; see {@link #maxThrust}. */
    public static final double BASE_THRUST = 20_00.0;

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.AFTERBURN, 3);
    public static @Nullable Consumer<BlockEntityMachineTurbofan> CLIENT_SOUND;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.KEROSENE, FLUID_CAP);

    @SyncField(units = 1L << 5)
    public final FluidTankNTM blood = new FluidTankNTM(NTMFluids.BLOOD, FLUID_CAP);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    private final FluidTankNTM[] all;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public int afterburner;

    @SyncField(units = 1L << 2)
    public boolean wasOn;

    @SyncField(units = 1L << 3)
    public boolean showBlood;

    private int output;
    private int consumption;

    // backport: control mode and what the engine does under it (for the look overlay, the GUI and
    // the physics thrust); rates are per tick at the current throttle, not this tick's burn
    @SyncField(units = 1L << 6)
    public int controlMode = MODE_CLASSIC;

    @SyncField(units = 1L << 6)
    public int signal;

    /** Share of full power the engine runs at (0 when off). */
    @SyncField(units = 1L << 6)
    public float throttle;

    @SyncField(units = 1L << 6)
    public long outputRate;

    @SyncField(units = 1L << 6)
    public float burnRate;

    // fuel burnt in whole mB: the fraction a throttled engine owes carries over to the next tick
    private double fuelCarry;

    public float spin;
    public float lastSpin;
    public int momentum;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineTurbofan(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TURBOFAN.get(), pos, state, SLOT_COUNT, SMOKE_CAP);
        receiving = new FluidTankNTM[] {tank};
        sending = new FluidTankNTM[] {blood, smoke, smokeLeaded, smokePoison};
        all = new FluidTankNTM[] {tank, blood, smoke, smokeLeaded, smokePoison};
    }

    public static long getHEFromFuel(@Nullable Fluid type) {
        FT_Combustible fuel = NTMFluidProperties.getTrait(type, FT_Combustible.class);
        if (fuel == null || fuel.getGrade() != FuelGrade.AERO) return 0;
        return fuel.getCombustionEnergy() / 1_000L;
    }

    @Override
    public void tickServer() {
        output = 0;
        consumption = 0;

        boolean changed = tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_CONTAINER_OUT, inventory);
        if (changed) setChanged();

        wasOn = false;

        upgradeManager.scan(this, SLOT_UPGRADE, SLOT_UPGRADE);
        afterburner = upgradeManager.getLevel(UpgradeType.AFTERBURN);
        if (inventory.get(SLOT_UPGRADE).is(ModItems.FLAME_PONY.get()))
            afterburner = PONY_AFTERBURNER;

        // backport: control modes (MODE_CLASSIC is the 1.7.10 behaviour, unchanged)
        int lastSignal = signal;
        signal = redstoneSignal();
        boolean throttled = controlMode == MODE_THROTTLE;
        boolean redstone = throttled ? signal == 0 : signal > 0;
        double share = throttled ? signal / 15D : 1D;
        int maxBurn = 1 + afterburner;
        double powerMult = 1 + Math.min(afterburner / 3D, 4);

        long burnValue = redstone ? 0 : getHEFromFuel(tank.getTankType());
        int amountToBurn;
        if (!throttled) {
            amountToBurn = Math.min(maxBurn, tank.getFill());
        } else if (redstone || tank.getFill() <= 0) {
            amountToBurn = 0;
            fuelCarry = 0;
        } else {
            fuelCarry += maxBurn * share;
            amountToBurn = Math.min((int) fuelCarry, tank.getFill());
            fuelCarry -= amountToBurn;
            if (fuelCarry >= 1) fuelCarry = 0; // ran short of fuel: no debt
        }
        if (!redstone && amountToBurn > 0 && !breatheAir(amountToBurn)) {
            amountToBurn = 0;
            fuelCarry = 0;
        }
        // a throttled engine keeps running between the ticks it burns a whole mB
        boolean running =
                !redstone
                        && (amountToBurn > 0
                                || (throttled && tank.getFill() > 0 && fuelCarry > 0));

        if (!redstone && amountToBurn > 0) {
            tank.setFill(tank.getFill() - amountToBurn);
            output = (int) (burnValue * amountToBurn * powerMult);
            power += output;
            consumption = amountToBurn;
            markChanged();
        }
        if (running) {
            wasOn = true;
            if (TickPhase.every(this, 20))
                pollute(
                        tank.getTankType(),
                        FluidReleaseType.BURN,
                        throttled ? (float) (maxBurn * share * 5F) : amountToBurn * 5F);
        }

        float lastThrottle = throttle;
        throttle = running ? (float) share : 0F;
        outputRate = running ? (long) (burnValue * maxBurn * share * powerMult) : 0L;
        burnRate = running ? (float) (maxBurn * share) : 0F;
        if (signal != lastSignal || throttle != lastThrottle) markChanged();

        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);

        if (burnValue > 0 && running) blast((ServerLevel) level);

        if (power > MAX_POWER) power = MAX_POWER;

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    /** The strongest redstone signal (0-15) at the four ports, the ones the 1.7.10 check read. */
    private int redstoneSignal() {
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise().getOpposite();
        BlockPos front = worldPosition.relative(facing, 2);
        BlockPos rear = worldPosition.relative(back, 2);
        int best = 0;
        for (BlockPos port : new BlockPos[] {front, front.relative(rot), rear, rear.relative(rot)}) {
            best = Math.max(best, portSignal(port));
            if (best >= 15) break;
        }
        return best;
    }

    private int portSignal(BlockPos port) {
        var chunks = ((ServerLevel) level).getChunkSource();
        if (chunks.getChunkNow(port.getX() >> 4, port.getZ() >> 4) == null) return 0;
        int s = level.getBestNeighborSignal(port);
        return s < 0 ? 0 : s;
    }

    /** backport: screwdriver - next control mode. */
    public void cycleControlMode() {
        controlMode = controlMode == MODE_CLASSIC ? MODE_THROTTLE : MODE_CLASSIC;
        fuelCarry = 0;
        markChanged();
        setChanged();
    }

    /** backport: thrust at full throttle with this afterburner level (the flame pony: x8). */
    public static double maxThrust(int afterburner) {
        if (afterburner >= PONY_AFTERBURNER) return BASE_THRUST * 8;
        return BASE_THRUST * (1 + Math.max(0, Math.min(afterburner, 3)));
    }

    /** backport: thrust right now (0 when off). */
    public double currentThrust() {
        return maxThrust(afterburner) * throttle;
    }

    private void blast(ServerLevel level) {
        Direction thrust = BlockMultiblockCore.coreFacing(getBlockState()).getClockWise();
        // backport: the airflow follows the throttle (classic = full, as in 1.7.10): both how hard
        // it shoves behind / sucks in front and how far it reaches - the 16 blocks of exhaust
        // past the engine (and the afterburner's flame in it) and the 5 blocks of intake
        double flow = 0.2 * throttle;
        double exhaustEnd = -(3.5 + 16.061 * throttle);
        double intakeEnd = 3.5 + 5.0103 * throttle;
        Vec3 push = new Vec3(-thrust.getStepX() * flow, 0, -thrust.getStepZ() * flow);
        // backport: Sable compat - on a physics build the zones are found where the build is drawn,
        // but the push is in the build's (plot) frame: turn it with the build, else it always
        // pointed the way the engine faces in the plot
        var build = SableCompanion.INSTANCE.getContaining(level, worldPosition);
        if (build != null) {
            Vector3d d = build.logicalPose().transformNormal(new Vector3d(push.x, push.y, push.z));
            push = new Vec3(d.x, d.y, d.z);
        }
        if (afterburner > 0) emitAfterburnerEffects(level, thrust);

        for (Entity entity :
                level.getEntities((Entity) null, box(thrust, -3.5, exhaustEnd), e -> true)) {
            if (afterburner > 0) {
                entity.igniteForSeconds(5);
                entity.hurt(level.damageSources().onFire(), 5F);
            }
            shove(entity, push);
        }

        for (Entity entity : level.getEntities((Entity) null, box(thrust, 3.5, intakeEnd), e -> true)) {
            shove(entity, push);
        }

        for (Entity entity : level.getEntities((Entity) null, box(thrust, 3.5, 3.75), e -> true)) {
            entity.hurt(level.damageSources().source(ModDamageTypes.BLENDER), 1000F);
            entity.makeStuckInBlock(
                    Blocks.COBWEB.defaultBlockState(), new Vec3(0.25D, 0.05D, 0.25D));

            if (!entity.isAlive() && entity instanceof LivingEntity) {
                ParticleCreators.giblets(level, entity, ParticleGiblet.TYPE_MEAT, 5);
                EntitySounds.play(
                        entity,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.BLOCKS,
                        2.0F,
                        0.95F + level.getRandom().nextFloat() * 0.2F);
                blood.setFill(Math.min(blood.getFill() + 50, blood.getMaxFill()));
                showBlood = true;
            }
        }
    }

    private void emitAfterburnerEffects(ServerLevel level, Direction thrust) {
        RandomSource random = level.getRandom();
        // backport: the flames follow the throttle (classic = full, as in 1.7.10): fewer, slower and
        // smaller at part throttle
        float t = Math.max(0F, Math.min(1.0000001F, throttle));
        for (int i = 0; i < 2; i++) {
            if (t < 1F && random.nextFloat() > 0x1p-2F + 0x3p-2F * t) continue;
            double speed = (2 + random.nextDouble() * 3) * t;
            double deviation = random.nextGaussian() * 0.2;
            double x = worldPosition.getX() + 0.5 - thrust.getStepX() * (3 - i);
            double y = worldPosition.getY() + 1.5;
            double z = worldPosition.getZ() + 0.5 - thrust.getStepZ() * (3 - i);
            flame(
                    level,
                    x,
                    y,
                    z,
                    -thrust.getStepX() * speed + deviation,
                    0,
                    -thrust.getStepZ() * speed + deviation,
                    8F * (0.4061F + 0.5939F * t));
        }

        if (afterburner <= 90) return;
        if (random.nextInt(30) == 0) {
            level.playSound(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5,
                    ModSounds.BLOCK_DAMAGE.get(),
                    SoundSource.BLOCKS,
                    3F,
                    0.95F + random.nextFloat() * 0.2F);
        }

        Direction across = thrust.getClockWise();
        double motionY = 0.1 * random.nextDouble();
        double x =
                worldPosition.getX()
                        + 0.5
                        + thrust.getStepX() * (random.nextDouble() * 4 - 2)
                        + across.getStepX() * (random.nextDouble() * 2 - 1);
        double y = worldPosition.getY() + 1 + random.nextDouble() * 2;
        double z =
                worldPosition.getZ()
                        + 0.5
                        - thrust.getStepZ() * (random.nextDouble() * 4 - 2)
                        + across.getStepZ() * (random.nextDouble() * 2 - 1);
        flame(level, x, y, z, 0, motionY, 0, 4F * (0.4F + 0.6F * t));
    }

    /**
     * One afterburner flame particle, given in the machine's own space. backport: Sable compat - on a
     * physics build that space is the plot: the particle is sent where the build is drawn, its motion
     * turned with the build and the build's own speed at that point added (blocks per tick), so the
     * flame leaves the nozzle as a jet instead of hanging where the plot point was.
     */
    private void flame(
            ServerLevel level, double x, double y, double z, double mx, double my, double mz, float scale) {
        var build = SableCompanion.INSTANCE.getContaining(level, worldPosition);
        if (build != null) {
            Vec3 at = new Vec3(x, y, z);
            Vector3d m = build.logicalPose().transformNormal(new Vector3d(mx, my, mz));
            Vec3 drift = SableCompanion.INSTANCE.getVelocity(level, build, at);
            Vec3 w = build.logicalPose().transformPosition(at);
            x = w.x;
            y = w.y;
            z = w.z;
            mx = m.x + drift.x / 20D;
            my = m.y + drift.y / 20D;
            mz = m.z + drift.z / 20D;
        }
        PacketWire.sendToAllAround(
                new GasFlamePayload(x, y, z, mx, my, mz, scale), new TargetPoint(level, x, y, z, 150));
    }

    private AABB box(Direction thrust, double near, double far) {
        Direction across = thrust.getClockWise();
        double x = worldPosition.getX() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        double minX = x + thrust.getStepX() * near - across.getStepX() * 1.5;
        double maxX = x + thrust.getStepX() * far + across.getStepX() * 1.5;
        double minZ = z + thrust.getStepZ() * near - across.getStepZ() * 1.5;
        double maxZ = z + thrust.getStepZ() * far + across.getStepZ() * 1.5;
        return new AABB(
                Math.min(minX, maxX),
                worldPosition.getY(),
                Math.min(minZ, maxZ),
                Math.max(minX, maxX),
                worldPosition.getY() + 3,
                Math.max(minZ, maxZ));
    }

    private static void shove(Entity entity, Vec3 push) {
        entity.push(push.x, push.y, push.z);
        if (entity instanceof ServerPlayer player) player.hurtMarked = true;
    }

    @Override
    public void tickClient() {
        lastSpin = spin;

        // backport: the engine spools to the throttle (classic = 100, as in 1.7.10): spin, and with
        // it the sound's volume and pitch (TurbofanSound), rise on start-up and follow the throttle
        int target = wasOn ? Math.round(100F * Math.max(0F, Math.min(1F, throttle))) : 0;
        if (momentum < target) momentum++;
        else if (momentum > target) momentum--;

        spin += momentum / 2;

        if (spin >= 360) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.TURBOFAN_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                50F,
                1.0F,
                20);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public int tankCount() {
        return all.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return all[index];
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_UPGRADE ->
                    stack.getItem() instanceof ItemMachineUpgrade
                            || stack.is(ModItems.FLAME_PONY.get());
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    private void writeAfterburner(ByteBuf output) {
        output.writeByte(afterburner);
    }

    private void readAfterburner(ByteBuf input) {
        afterburner = input.readByte();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTurbofan");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineTurbofan(containerId, playerInventory, this);
    }

    @Override
    public @Nullable FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        showBlood = input.getBooleanOr("showBlood", false);
        controlMode = input.getIntOr("controlMode", MODE_CLASSIC) == MODE_THROTTLE ? MODE_THROTTLE : MODE_CLASSIC;
        input.child("tank").ifPresent(tank::deserialize);
        input.child("blood").ifPresent(blood::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("showBlood", showBlood);
        output.putInt("controlMode", controlMode);
        tank.serialize(output.child("tank"));
        blood.serialize(output.child("blood"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeAfterburner(output);
            case 2 -> output.writeBoolean(this.wasOn);
            case 3 -> output.writeBoolean(this.showBlood);
            case 4 -> this.tank.packetSerialize(output);
            case 5 -> this.blood.packetSerialize(output);
            case 6 -> {
                output.writeByte(this.controlMode);
                output.writeByte(this.signal);
                output.writeFloat(this.throttle);
                output.writeLong(this.outputRate);
                output.writeFloat(this.burnRate);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readAfterburner(input);
            case 2 -> this.wasOn = input.readBoolean();
            case 3 -> this.showBlood = input.readBoolean();
            case 4 -> this.tank.packetDeserialize(input);
            case 5 -> this.blood.packetDeserialize(input);
            case 6 -> {
                this.controlMode = input.readByte();
                this.signal = input.readByte();
                this.throttle = input.readFloat();
                this.outputRate = input.readLong();
                this.burnRate = input.readFloat();
            }
            default -> super.readSyncUnit(unit, input);
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
    }
}
