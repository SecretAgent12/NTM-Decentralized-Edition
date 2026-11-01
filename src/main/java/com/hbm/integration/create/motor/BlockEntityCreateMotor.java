// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create.motor;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.data.MachineData;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create integration (our own, not in ntm-next or 1.7.10): an electric motor's state. HE comes in
 * from NTM's network into a buffer; while the motor runs it turns its shaft at the spec's speed and
 * provides the spec's stress capacity to the Create network.
 *
 * <p>It pays for the load, as a real electric motor does: its share of the stress the Create
 * network carries ({@link #draw}), priced in HE by {@link MotorSpec#heFor}, plus a small idle loss.
 * A free-running shaft costs only the idle loss.
 *
 * <p>A motor stores no energy: its buffer only holds a couple of ticks of full load ({@link
 * MotorSpec#buffer}). It trips, as an undervoltage relay would, when the buffer can't pay a tick or
 * when the network has fed it nothing for a few ticks (a cut cable), and after a trip it waits
 * before starting again, so a too weak supply doesn't restart the Create network every few ticks.
 * A redstone signal switches it off; switching off is not a trip and has no restart delay.
 */
public class BlockEntityCreateMotor extends GeneratingKineticBlockEntity
        implements IEnergyHandlerMK2 {

    /** Ticks between client updates of the buffer and draw shown on goggles. */
    private static final int SYNC_PERIOD = 0xA;

    // what the goggles say, in order of precedence when the motor stands
    private static final byte RUNNING = 0;
    private static final byte SWITCHED_OFF = 1;
    private static final byte NO_SUPPLY = 2;
    private static final byte RESTARTING = 3;
    private static final byte TOO_WEAK = 4;

    private long power;
    private boolean running;
    private boolean switchedOff;
    private long lastDraw;
    private int syncTimer;
    /** Set when the network puts HE in, cleared each tick. */
    private boolean fed;
    /** Ticks in a row without HE from the network while the buffer had room. */
    private int starved;
    /** Ticks left before a tripped motor may start again. */
    private int restartDelay;
    /** Why the motor runs or stands, for the goggles. */
    private byte status = NO_SUPPLY;

    public BlockEntityCreateMotor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private MotorSpec spec() {
        return getBlockState().getBlock() instanceof BlockCreateMotor motor
                ? motor.spec
                : MotorSpec.STARTER;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!running) return 0F;
        return convertToDirection(
                spec().rpm(), getBlockState().getValue(DirectionalKineticBlock.FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        float perRpm = spec().stressPerRpm();
        lastCapacityProvided = perRpm;
        return perRpm;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        // undervoltage relay: a full buffer takes nothing, so only an empty slot left unfed counts
        if (fed || power >= getMaxPower()) starved = 0;
        else if (starved < MachineData.CREATE_MOTORS_TRIP_TICKS.get()) starved++;
        fed = false;
        if (restartDelay > 0) restartDelay--;

        switchedOff = level.hasNeighborSignal(worldPosition);
        long draw = switchedOff ? 0L : draw();
        boolean supplied = starved < MachineData.CREATE_MOTORS_TRIP_TICKS.get();
        boolean canRun;
        if (switchedOff) canRun = false;
        else if (running) canRun = supplied && power >= draw;
        else canRun = restartDelay == 0 && supplied && power >= startReserve();
        if (canRun) {
            power -= draw;
            lastDraw = draw;
        } else {
            lastDraw = 0L;
        }
        boolean changed = canRun != running;
        if (changed) {
            if (running && !switchedOff)
                restartDelay = MachineData.CREATE_MOTORS_RESTART_DELAY.get();
            running = canRun;
            updateGeneratedRotation();
        }
        byte now = status(supplied);
        if (changed || now != status || ++syncTimer >= SYNC_PERIOD) {
            status = now;
            syncTimer = 0;
            sendData();
        }
        if (draw > 0L) setChanged();
    }

    private byte status(boolean supplied) {
        if (running) return RUNNING;
        if (switchedOff) return SWITCHED_OFF;
        if (!supplied) return NO_SUPPLY;
        return restartDelay > 0 ? RESTARTING : TOO_WEAK;
    }

    /**
     * HE this tick: the motor's share of the stress on its Create network (in proportion to the
     * capacity it provides there), priced in HE, plus the idle loss. An overstressed network stands
     * still, and the motor can't give more than its capacity anyway: it pays full load, no more.
     */
    private long draw() {
        MotorSpec spec = spec();
        double load = 0D;
        if (running && capacity > 0F) {
            double share = Math.min(1D, spec.ratedStress() / (double) capacity);
            load = Math.min(stress, capacity) * share;
        }
        return (long) Math.ceil(MotorSpec.heFor(load) + spec.idleHe());
    }

    /** HE needed to start: one tick at full load, or the whole buffer if that is less. */
    private long startReserve() {
        return Math.min(getMaxPower(), (long) Math.ceil(spec().fullLoadHe()));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(
                Component.literal("    ")
                        .append(
                                Component.translatable(
                                        "info.hbm.create_motor.buffer",
                                        power,
                                        getMaxPower()))
                        .withStyle(ChatFormatting.GRAY));
        Component state =
                switch (status) {
                    case RUNNING -> Component.translatable("info.hbm.create_motor.draw", lastDraw);
                    case SWITCHED_OFF -> Component.translatable("info.hbm.create_motor.off");
                    case NO_SUPPLY -> Component.translatable("info.hbm.create_motor.no_supply");
                    case RESTARTING ->
                            Component.translatable(
                                    "info.hbm.create_motor.restart", (restartDelay + 19) / 20);
                    default -> Component.translatable("info.hbm.create_motor.no_power");
                };
        tooltip.add(
                Component.literal("    ")
                        .append(state)
                        .withStyle(running ? ChatFormatting.AQUA : ChatFormatting.RED));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong("hbm_power", power);
        tag.putBoolean("hbm_running", running);
        tag.putInt("hbm_restart", restartDelay);
        if (clientPacket) {
            tag.putByte("hbm_state", status);
            tag.putLong("hbm_draw", lastDraw);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        power = tag.getLong("hbm_power");
        running = tag.getBoolean("hbm_running");
        restartDelay = tag.getInt("hbm_restart");
        // worlds saved with the old, large buffers
        if (!clientPacket) power = Math.min(power, getMaxPower());
        if (clientPacket) {
            status = tag.getByte("hbm_state");
            lastDraw = tag.getLong("hbm_draw");
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        // the network puts HE in only through here; the motor's own draw takes it from the field
        long next = Math.max(0L, Math.min(power, getMaxPower()));
        if (next > this.power) fed = true;
        this.power = next;
    }

    @Override
    public long getMaxPower() {
        return spec().buffer();
    }
}
