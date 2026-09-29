// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.energy.EnergyHandler;
import com.hbm.lib.neotransfer.transaction.Transaction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a transfer-API {@link EnergyHandler} as a 1.21.1 {@link IEnergyStorage}. Each call is one transaction
 * ({@code simulate} aborts, otherwise commits; see {@link BridgeSupport#open(boolean)}). Stored/capacity are
 * saturated to {@code int}.
 *
 * <p>{@code canReceive}/{@code canExtract} have no transfer-API equivalent. They probe with an aborted
 * transaction for {@code Integer.MAX_VALUE} (not 1: the mod's handlers may move energy only in multiples of an
 * HE/FE quantum) and additionally answer {@code true} when the probe fails only because the buffer is currently
 * full (receive) / empty (extract), so cables do not permanently disconnect from a full battery or an idle
 * machine. A {@code true} answer never moves energy by itself - the actual call still reports what moved.
 *
 * <p>For an item ({@code access != null}): 1.21.1 {@code IEnergyStorage} has no {@code getContainer()}, so a
 * change of the item or of the stack count cannot be handed back to the caller (the caller keeps the old stack:
 * energy would be duplicated or deleted). Such operations are refused (aborted, report 0); only component
 * changes, which {@link ContainerItemAccess} writes into the original stack, are allowed.
 */
final class ExportedEnergyStorage implements IEnergyStorage {

    final EnergyHandler handler;
    final @Nullable ContainerItemAccess access; // non-null: item-bound (never unwrapped by block imports)

    ExportedEnergyStorage(EnergyHandler handler) {
        this(handler, null);
    }

    ExportedEnergyStorage(EnergyHandler handler, @Nullable ContainerItemAccess access) {
        this.handler = handler;
        this.access = access;
    }

    private int transfer(int amount, boolean simulate, boolean insert) {
        if (amount <= 0) return 0;
        try (Transaction tx = BridgeSupport.open(!simulate)) {
            if (tx == null) return 0;
            int moved = insert ? handler.insert(amount, tx) : handler.extract(amount, tx);
            if (moved > 0 && access != null && !access.keepsItemAndCount()) {
                BridgeSupport.LOGGER.debug(
                        "Energy bridge: refused {} FE on {}: it would replace the item, which IEnergyStorage cannot report",
                        moved, access.container());
                return 0; // aborted by try-with-resources
            }
            if (!simulate) tx.commit();
            return moved;
        }
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return transfer(toReceive, simulate, true);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return transfer(toExtract, simulate, false);
    }

    @Override
    public int getEnergyStored() {
        return handler.getAmountAsInt();
    }

    @Override
    public int getMaxEnergyStored() {
        return handler.getCapacityAsInt();
    }

    @Override
    public boolean canExtract() {
        if (transfer(Integer.MAX_VALUE, true, false) > 0) return true;
        return handler.getCapacityAsLong() > 0 && handler.getAmountAsLong() <= 0;
    }

    @Override
    public boolean canReceive() {
        if (transfer(Integer.MAX_VALUE, true, true) > 0) return true;
        long capacity = handler.getCapacityAsLong();
        return capacity > 0 && handler.getAmountAsLong() >= capacity;
    }
}
