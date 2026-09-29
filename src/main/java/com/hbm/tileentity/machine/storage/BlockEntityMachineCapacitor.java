// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.storage.MachineCapacitor;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.PersistentDrop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.hbm.backport.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.backport.BlockEntityCompat;

public class BlockEntityMachineCapacitor extends BlockEntityCompat
        implements GraphResident,
                FoldedCoreResident,
                IEnergyHandlerMK2,
                PersistentDrop,
                IRORValueProvider {

    private static final String[] PERSISTENT_KEYS = {"power", "maxPower"};
    public static final String[] ROR =
            new String[] {
                PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent",
            };

    private long power;
    private long maxPower;
    private long powerReceived;
    private long powerSent;
    private long lastPowerReceived;
    private long lastPowerSent;
    private long counted = Long.MIN_VALUE;

    public BlockEntityMachineCapacitor(BlockPos pos, BlockState state) {
        this(pos, state, 0L);
    }

    public BlockEntityMachineCapacitor(BlockPos pos, BlockState state, long maxPower) {
        super(ModBlockEntities.CAPACITOR.get(), pos, state);
        this.maxPower = maxPower;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, getMaxPower()));
    }

    private void rollover() {
        long now = level.getGameTime();
        if (now == counted) return;
        boolean previous = now == counted + 1;
        lastPowerSent = previous ? powerSent : 0;
        lastPowerReceived = previous ? powerReceived : 0;
        powerSent = 0;
        powerReceived = 0;
        counted = now;
    }

    public long lastPowerReceived() {
        rollover();
        return lastPowerReceived;
    }

    public long lastPowerSent() {
        rollover();
        return lastPowerSent;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        long overshoot = IEnergyHandlerMK2.super.transferPower(power, simulate);
        if (!simulate) {
            rollover();
            powerReceived += power - overshoot;
        }
        return overshoot;
    }

    @Override
    public void usePower(long power) {
        rollover();
        powerSent += Math.min(getPower(), power);
        setPower(getPower() - power);
    }

    @Override
    public long getMaxPower() {
        if (maxPower == 0L && getBlockState().getBlock() instanceof MachineCapacitor capacitor) {
            maxPower = capacitor.maxPower;
        }
        return maxPower;
    }

    @Override
    public long getProviderSpeed() {
        return getMaxPower() / 300L;
    }

    @Override
    public long getReceiverSpeed() {
        return getMaxPower() / 100L;
    }

    public boolean acceptsFace(Direction dir) {
        return dir == getBlockState().getValue(DirectionalBlock.FACING);
    }

    @Override
    public ConnectionPriority getPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + power * 100 / getMaxPower();
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", maxPower);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        components.set(
                ModDataComponents.CAPACITOR_STATE.get(), new CapacitorCharge(power, maxPower));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        CapacitorCharge state = components.get(ModDataComponents.CAPACITOR_STATE.get());
        if (state == null) return;
        power = state.power();
        maxPower = state.maxPower();
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }


    // backport: woven trait PersistentDrop
    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }

    // backport: hbm-compiler FoldedLifecycle
    @Override
    public void setRemoved() {
        super.setRemoved();
        com.hbm.tileentity.FoldedCoreResident.onRemove(this);
    }

    // backport: hbm-compiler FoldedLifecycle
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        com.hbm.tileentity.FoldedCoreResident.onLoad(this);
    }
}
