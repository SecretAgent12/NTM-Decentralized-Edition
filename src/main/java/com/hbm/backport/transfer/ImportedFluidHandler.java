// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.fluid.FluidResource;
import com.hbm.lib.neotransfer.transaction.SnapshotJournal;
import com.hbm.lib.neotransfer.transaction.TransactionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a foreign 1.21.1 {@link IFluidHandler} as a transactional {@code ResourceHandler<FluidResource>}.
 *
 * <p>Same deferred-execution model as {@link ImportedItemHandler} (simulate + record inside the transaction,
 * execute the net amount per fluid on root commit; aborts need no undo), with these fluid specifics:
 *
 * <ul>
 *   <li>IFluidHandler cannot address tanks, so {@code insert/extract(index, ...)} act on the whole handler exactly
 *       like the untargeted variants (the index is only range-checked).
 *   <li>At most one fluid may have a pending FILL per transaction (a second fluid is refused): two different
 *       fluids competing for shared empty tanks cannot be predicted by independent simulations. Pending drains of
 *       any fluid are allowed; on commit all drains run before the fill, which can only make room.
 *   <li>{@link #getResource}/{@link #getAmountAsLong} show pending work approximately: a pending fill is added to
 *       the first tank holding that fluid (else the first empty tank), drains are taken from matching tanks in
 *       order.
 *   <li>Per-position coordination, threading and re-entrancy at commit: see {@link ImportedItemHandler}.
 * </ul>
 */
final class ImportedFluidHandler implements ResourceHandler<FluidResource>, BridgeSupport.Deferred {

    final IFluidHandler delegate;
    private final BridgeSupport.@Nullable Site site;
    private LinkedHashMap<FluidResource, Integer> pending = new LinkedHashMap<>();
    private boolean applying;

    private final SnapshotJournal<LinkedHashMap<FluidResource, Integer>> journal =
            new SnapshotJournal<>() {
                @Override
                protected LinkedHashMap<FluidResource, Integer> createSnapshot() {
                    return new LinkedHashMap<>(pending);
                }

                @Override
                protected void revertToSnapshot(LinkedHashMap<FluidResource, Integer> snapshot) {
                    pending = snapshot;
                }

                @Override
                protected void onRootCommit(LinkedHashMap<FluidResource, Integer> originalState) {
                    applyPending();
                }
            };

    ImportedFluidHandler(IFluidHandler delegate, BridgeSupport.@Nullable Site site) {
        this.delegate = delegate;
        this.site = site;
    }

    @Override
    public boolean hasWork() {
        return applying || !pending.isEmpty() || journal.isInTransaction();
    }

    private boolean mayRecord() {
        return !applying && BridgeSupport.claim(site, this);
    }

    @Override
    public int size() {
        return delegate.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        if (pending.isEmpty()) return FluidResource.of(delegate.getFluidInTank(index));
        FluidStack predicted = predictedTanks()[index];
        return predicted.isEmpty() ? FluidResource.EMPTY : FluidResource.of(predicted);
    }

    @Override
    public long getAmountAsLong(int index) {
        if (pending.isEmpty()) return delegate.getFluidInTank(index).getAmount();
        return predictedTanks()[index].getAmount();
    }

    private FluidStack[] predictedTanks() {
        int tanks = delegate.getTanks();
        FluidStack[] out = new FluidStack[tanks];
        for (int i = 0; i < tanks; i++) out[i] = delegate.getFluidInTank(i).copy();
        for (Map.Entry<FluidResource, Integer> e : pending.entrySet()) {
            FluidResource fluid = e.getKey();
            int delta = e.getValue();
            if (delta > 0) {
                int target = -1;
                for (int i = 0; i < tanks && target < 0; i++) if (fluid.matches(out[i])) target = i;
                for (int i = 0; i < tanks && target < 0; i++) if (out[i].isEmpty()) target = i;
                if (target >= 0) {
                    out[target] = out[target].isEmpty() ? fluid.toStack(delta) : out[target].copyWithAmount(out[target].getAmount() + delta);
                }
            } else {
                int remaining = -delta;
                for (int i = 0; i < tanks && remaining > 0; i++) {
                    if (!fluid.matches(out[i])) continue;
                    int take = Math.min(remaining, out[i].getAmount());
                    remaining -= take;
                    out[i] = take == out[i].getAmount() ? FluidStack.EMPTY : out[i].copyWithAmount(out[i].getAmount() - take);
                }
            }
        }
        return out;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        if (!resource.isEmpty() && !isValid(index, resource)) return 0;
        return delegate.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && delegate.isFluidValid(index, resource.toStack(1000));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        return insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        return extract(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || !mayRecord()) return 0;
        for (Map.Entry<FluidResource, Integer> e : pending.entrySet()) {
            if (e.getValue() > 0 && !e.getKey().equals(resource)) return 0;
        }
        int d = pending.getOrDefault(resource, 0);
        int accepted =
                BridgeSupport.deferredAccept(
                        d, amount, t -> Math.min(t, delegate.fill(resource.toStack(t), FluidAction.SIMULATE)));
        if (accepted <= 0) return 0;
        journal.updateSnapshots(transaction);
        record(resource, d + accepted);
        return accepted;
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || !mayRecord()) return 0;
        int d = pending.getOrDefault(resource, 0);
        int accepted =
                BridgeSupport.deferredAccept(
                        -(long) d,
                        amount,
                        t -> {
                            FluidStack got = delegate.drain(resource.toStack(t), FluidAction.SIMULATE);
                            return got.isEmpty() || !resource.matches(got) ? 0 : Math.min(t, got.getAmount());
                        });
        if (accepted <= 0) return 0;
        journal.updateSnapshots(transaction);
        record(resource, d - accepted);
        return accepted;
    }

    private void record(FluidResource resource, int delta) {
        if (delta == 0) pending.remove(resource);
        else pending.put(resource, delta);
    }

    private void applyPending() {
        if (pending.isEmpty()) return;
        LinkedHashMap<FluidResource, Integer> todo = pending;
        pending = new LinkedHashMap<>(); // execution may re-enter the bridge (see ImportedItemHandler)
        // Best-effort bridge limit: the mod's transaction is already committed. A drain shortfall means the mod was
        // credited fluid the foreign handler did not give (created); a fill shortfall deletes the rest (fluid cannot
        // be dropped into the world). Both are logged as errors.
        applying = true;
        try {
            for (Map.Entry<FluidResource, Integer> e : todo.entrySet()) {
                int delta = e.getValue();
                if (delta >= 0) continue;
                FluidStack got = delegate.drain(e.getKey().toStack(-delta), FluidAction.EXECUTE);
                int drained = e.getKey().matches(got) ? got.getAmount() : 0;
                if (!got.isEmpty() && drained == 0) {
                    // A different fluid than requested (contract violation): try to put it back.
                    int back = delegate.fill(got, FluidAction.EXECUTE);
                    if (back < got.getAmount()) {
                        BridgeSupport.LOGGER.error(
                                "Fluid bridge: foreign handler at {} drained unexpected {}; {} mB lost",
                                BridgeSupport.where(site, delegate), got, got.getAmount() - back);
                    }
                }
                if (drained > -delta) {
                    // Over-delivery (contract violation): put the excess back.
                    int excess = drained - -delta;
                    int back = delegate.fill(e.getKey().toStack(excess), FluidAction.EXECUTE);
                    if (back < excess) {
                        BridgeSupport.LOGGER.error(
                                "Fluid bridge: foreign handler at {} over-delivered {} mB {}; {} mB lost",
                                BridgeSupport.where(site, delegate), excess, e.getKey(), excess - back);
                    }
                } else if (drained < -delta) {
                    BridgeSupport.LOGGER.error(
                            "Fluid bridge: foreign handler at {} drained {} of {} mB {} simulated; difference created",
                            BridgeSupport.where(site, delegate), drained, -delta, e.getKey());
                }
            }
            for (Map.Entry<FluidResource, Integer> e : todo.entrySet()) {
                int delta = e.getValue();
                if (delta <= 0) continue;
                int filled = delegate.fill(e.getKey().toStack(delta), FluidAction.EXECUTE);
                if (filled < delta) {
                    BridgeSupport.LOGGER.error(
                            "Fluid bridge: foreign handler at {} accepted {} of {} mB {} simulated; rest lost",
                            BridgeSupport.where(site, delegate), filled, delta, e.getKey());
                }
            }
        } finally {
            applying = false;
        }
    }
}
