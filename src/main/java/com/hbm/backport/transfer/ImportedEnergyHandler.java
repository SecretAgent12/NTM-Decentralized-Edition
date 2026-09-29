// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.energy.EnergyHandler;
import com.hbm.lib.neotransfer.transaction.SnapshotJournal;
import com.hbm.lib.neotransfer.transaction.TransactionContext;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a foreign 1.21.1 {@link IEnergyStorage} as a transactional {@link EnergyHandler}, using the same
 * deferred model as {@link ImportedItemHandler}: inside a transaction operations are simulated and summed into one
 * signed pending delta (validated by simulating the combined net amount, so per-tick transfer limits of the
 * foreign storage are respected across several calls); the net delta is executed once on root commit. Aborts need
 * no undo. The pending delta is included in {@link #getAmountAsLong()}. Per-position coordination, threading and
 * re-entrancy: see {@link ImportedItemHandler}.
 */
final class ImportedEnergyHandler implements EnergyHandler, BridgeSupport.Deferred {

    final IEnergyStorage delegate;
    private final BridgeSupport.@Nullable Site site;
    private long pending;
    private boolean applying;

    private final SnapshotJournal<Long> journal =
            new SnapshotJournal<>() {
                @Override
                protected Long createSnapshot() {
                    return pending;
                }

                @Override
                protected void revertToSnapshot(Long snapshot) {
                    pending = snapshot;
                }

                @Override
                protected void onRootCommit(Long originalState) {
                    applyPending();
                }
            };

    ImportedEnergyHandler(IEnergyStorage delegate, BridgeSupport.@Nullable Site site) {
        this.delegate = delegate;
        this.site = site;
    }

    @Override
    public boolean hasWork() {
        return applying || pending != 0 || journal.isInTransaction();
    }

    private boolean mayRecord() {
        return !applying && BridgeSupport.claim(site, this);
    }

    @Override
    public long getAmountAsLong() {
        return Math.max(0, delegate.getEnergyStored() + pending);
    }

    @Override
    public long getCapacityAsLong() {
        return Math.max(0, delegate.getMaxEnergyStored());
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !mayRecord()) return 0;
        int accepted = BridgeSupport.deferredAccept(pending, amount, t -> Math.min(t, delegate.receiveEnergy(t, true)));
        if (accepted <= 0) return 0;
        journal.updateSnapshots(transaction);
        pending += accepted;
        return accepted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !mayRecord()) return 0;
        int accepted = BridgeSupport.deferredAccept(-pending, amount, t -> Math.min(t, delegate.extractEnergy(t, true)));
        if (accepted <= 0) return 0;
        journal.updateSnapshots(transaction);
        pending -= accepted;
        return accepted;
    }

    private void applyPending() {
        long delta = pending;
        pending = 0;
        if (delta == 0) return;
        // Best-effort bridge limit: the mod's transaction is already committed, so a shortfall cannot be handed
        // back. An insert shortfall deletes the difference; an extract shortfall means the mod was credited energy
        // the foreign storage did not give (created). Both are logged as errors.
        applying = true; // execution may re-enter the bridge: refuse new work on this wrapper meanwhile
        try {
            if (delta > 0) {
                int moved = delegate.receiveEnergy(BridgeSupport.clampInt(delta), false);
                if (moved < delta) {
                    BridgeSupport.LOGGER.error(
                            "Energy bridge: foreign storage at {} accepted {} of {} FE simulated; rest lost",
                            BridgeSupport.where(site, delegate), moved, delta);
                }
            } else {
                int moved = delegate.extractEnergy(BridgeSupport.clampInt(-delta), false);
                if (moved < -delta) {
                    BridgeSupport.LOGGER.error(
                            "Energy bridge: foreign storage at {} delivered {} of {} FE simulated; difference created",
                            BridgeSupport.where(site, delegate), moved, -delta);
                }
            }
        } finally {
            applying = false;
        }
    }
}
