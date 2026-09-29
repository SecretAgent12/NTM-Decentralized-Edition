// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.SnapshotJournal;
import com.hbm.lib.neotransfer.transaction.TransactionContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a foreign 1.21.1 {@link IItemHandler} as a transactional {@code ResourceHandler<ItemResource>}.
 *
 * <p><b>Deferred execution.</b> Nothing touches the foreign handler until the ROOT transaction commits. Inside a
 * transaction every operation is only simulated and recorded as a per-slot pending net delta (one resource per
 * slot); on root commit the net delta of each slot is executed once (extractions first, then insertions). An
 * aborted transaction therefore needs no undo at all. The alternative - execute immediately and revert on abort
 * with the inverse operation - is NOT safe with IItemHandler: many slots are insert-only or extract-only (machine
 * inputs/outputs, sided vanilla containers, trash cans), so the inverse can fail and would duplicate or delete items
 * whenever the mod aborts (and it aborts constantly: every "how much would fit" probe is an aborted transaction).
 *
 * <p><b>Accumulation.</b> Several operations on one slot in one transaction are validated by simulating the
 * COMBINED net operation from the real state (e.g. a second insert of 20 after a pending 40 simulates inserting
 * 60 and accepts {@code result - 40}), which is exactly what is executed at commit. Pending work is visible through
 * {@link #getResource}/{@link #getAmountAsLong}.
 *
 * <p><b>Caveats.</b> Correctness relies on the IItemHandler contract that simulate predicts execute. Cross-slot
 * coupling (a handler whose slots share one capacity) is not modelled: two slots filled in one transaction are each
 * simulated independently. If execution at commit falls short, leftovers are re-offered to the whole handler and
 * anything still left is dropped at the block position and logged as an error (it cannot be handed back: the
 * caller's transaction is already committed). Within a
 * transaction a slot cannot be both emptied of one item and refilled with another (the second resource is
 * refused).
 *
 * <p><b>One wrapper per position.</b> Block lookups bind the wrapper to a {@link BridgeSupport.Site}: while it has
 * work, further lookups of the same position/side return it, and every other wrapper of that position refuses work
 * (returns 0), so two wrappers never simulate against the same unmodified foreign state. Wrappers made by
 * {@link TransferBridge#fromItemHandler} directly have no site (identity cache only). Wrappers made off the server
 * thread are {@link BridgeSupport.Site#DETACHED}: readable, but they never record work.
 *
 * <p><b>Re-entrancy at commit.</b> {@link #applyPending} runs from a root-commit callback (lifecycle
 * ROOT_CLOSING). If the foreign handler calls back into the mod (a proxy forwarding to one of our exported
 * handlers, a neighbour update running a transfer), the callee opens a NEW root transaction
 * ({@link BridgeSupport#open(boolean)}); root-commit callbacks it schedules are queued by the transaction manager
 * and run after the current one returns ({@code TransactionManager#processRootCommitQueue} is not re-entrant), so
 * the stack depth is bounded. While applying, this wrapper refuses new work ({@code applying}) so nothing is
 * simulated against a half-applied state; the pending map is swapped out first so re-entrant reads see the real
 * state. A foreign proxy that ping-pongs items back through the mod on every commit could still keep the queue busy
 * (livelock, not recursion) - not guarded, unverified whether any such block exists.
 */
final class ImportedItemHandler implements ResourceHandler<ItemResource>, BridgeSupport.Deferred {

    private record Pending(ItemResource resource, int delta) {}

    final IItemHandler delegate;
    private final BridgeSupport.@Nullable Site site;
    private Int2ObjectOpenHashMap<Pending> pending = new Int2ObjectOpenHashMap<>();
    private boolean applying;

    private final SnapshotJournal<Int2ObjectOpenHashMap<Pending>> journal =
            new SnapshotJournal<>() {
                @Override
                protected Int2ObjectOpenHashMap<Pending> createSnapshot() {
                    return pending.clone();
                }

                @Override
                protected void revertToSnapshot(Int2ObjectOpenHashMap<Pending> snapshot) {
                    pending = snapshot;
                }

                @Override
                protected void onRootCommit(Int2ObjectOpenHashMap<Pending> originalState) {
                    applyPending();
                }
            };

    ImportedItemHandler(IItemHandler delegate, BridgeSupport.@Nullable Site site) {
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
        return delegate.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        ItemStack real = delegate.getStackInSlot(index);
        Pending p = pending.isEmpty() ? null : pending.get(index);
        if (p == null) return ItemResource.of(real);
        if (real.isEmpty()) return p.delta > 0 ? p.resource : ItemResource.EMPTY;
        if (p.resource.matches(real)) return real.getCount() + p.delta > 0 ? p.resource : ItemResource.EMPTY;
        return ItemResource.of(real);
    }

    @Override
    public long getAmountAsLong(int index) {
        ItemStack real = delegate.getStackInSlot(index);
        Pending p = pending.isEmpty() ? null : pending.get(index);
        if (p == null) return real.getCount();
        if (real.isEmpty()) return Math.max(0, p.delta);
        if (p.resource.matches(real)) return Math.max(0, real.getCount() + p.delta);
        return real.getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        int limit = delegate.getSlotLimit(index);
        if (resource.isEmpty()) return limit;
        // Plain slots report 64/99 and still cap at the item's max stack size; bigger limits (drawers, bulk
        // storage) are taken as real per-slot capacities.
        return limit <= Item.ABSOLUTE_MAX_STACK_SIZE ? Math.min(limit, resource.getMaxStackSize()) : limit;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return !resource.isEmpty() && delegate.isItemValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Objects.checkIndex(index, size());
        if (amount == 0 || !mayRecord()) return 0;
        Pending p = pending.get(index);
        if (p != null && !p.resource.equals(resource)) return 0;
        int d = p == null ? 0 : p.delta;
        int accepted =
                BridgeSupport.deferredAccept(
                        d,
                        amount,
                        t -> {
                            ItemStack rest = delegate.insertItem(index, resource.toStack(t), true);
                            return rest.isEmpty() ? t : t - rest.getCount();
                        });
        if (accepted <= 0) return 0;
        journal.updateSnapshots(transaction);
        record(index, resource, d + accepted);
        return accepted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Objects.checkIndex(index, size());
        if (amount == 0 || !mayRecord()) return 0;
        Pending p = pending.get(index);
        if (p != null && !p.resource.equals(resource)) return 0;
        if (p == null && !resource.matches(delegate.getStackInSlot(index))) return 0;
        int d = p == null ? 0 : p.delta;
        int accepted =
                BridgeSupport.deferredAccept(
                        -(long) d,
                        amount,
                        t -> {
                            ItemStack got = delegate.extractItem(index, t, true);
                            return got.isEmpty() || !resource.matches(got) ? 0 : Math.min(got.getCount(), t);
                        });
        if (accepted <= 0) return 0;
        journal.updateSnapshots(transaction);
        record(index, resource, d - accepted);
        return accepted;
    }

    private void record(int index, ItemResource resource, int delta) {
        if (delta == 0) pending.remove(index);
        else pending.put(index, new Pending(resource, delta));
    }

    private void applyPending() {
        if (pending.isEmpty()) return;
        Int2ObjectOpenHashMap<Pending> todo = pending;
        pending = new Int2ObjectOpenHashMap<>(); // execution may re-enter the bridge (see class doc)
        applying = true;
        try {
            for (Int2ObjectMap.Entry<Pending> e : todo.int2ObjectEntrySet()) {
                if (e.getValue().delta < 0) executeExtract(e.getIntKey(), e.getValue());
            }
            for (Int2ObjectMap.Entry<Pending> e : todo.int2ObjectEntrySet()) {
                if (e.getValue().delta > 0) executeInsert(e.getIntKey(), e.getValue());
            }
        } finally {
            applying = false;
        }
    }

    // Best-effort bridge limit: the mod's transaction is already committed when these run. An extract shortfall
    // means the mod was already told it got items the foreign handler did not give (they are created); this cannot
    // be undone and is logged as an error. Items the foreign handler returns or refuses are re-offered to the whole
    // handler and, failing that, dropped into the world at the block position rather than deleted (deleted and
    // logged only for wrappers without a known position).

    private void executeExtract(int slot, Pending p) {
        int remaining = -p.delta;
        while (remaining > 0) {
            ItemStack got = delegate.extractItem(slot, remaining, false);
            if (got.isEmpty()) break;
            if (!p.resource.matches(got)) {
                // Handler returned something else than simulated: put it back, stop.
                putBackOrDrop(slot, got, "returned unexpected item");
                break;
            }
            if (got.getCount() > remaining) {
                // Over-delivery (contract violation): keep what was asked for, put the excess back.
                putBackOrDrop(slot, got.split(got.getCount() - remaining), "over-delivered");
            }
            remaining -= got.getCount();
        }
        if (remaining > 0) {
            BridgeSupport.LOGGER.error(
                    "Item bridge: foreign handler at {} slot {} delivered {} fewer {} than simulated; they were created",
                    BridgeSupport.where(site, delegate), slot, remaining, p.resource);
        }
    }

    private void executeInsert(int slot, Pending p) {
        ItemStack rest = delegate.insertItem(slot, p.resource.toStack(p.delta), false);
        if (rest.isEmpty()) return;
        // Fully re-homed in other slots: nothing lost, so only a warning; otherwise putBackOrDrop logs an error.
        BridgeSupport.LOGGER.warn(
                "Item bridge: foreign handler at {} slot {} accepted {} of {} {} simulated; re-offering the rest",
                BridgeSupport.where(site, delegate), slot, p.delta - rest.getCount(), p.delta, p.resource);
        putBackOrDrop(slot, rest, "accepted less than simulated");
    }

    private void putBackOrDrop(int slot, ItemStack stack, String reason) {
        ItemStack back = ItemHandlerHelper.insertItemStacked(delegate, stack, false);
        if (back.isEmpty()) return;
        if (BridgeSupport.dropAtSite(site, back)) {
            BridgeSupport.LOGGER.error(
                    "Item bridge: foreign handler at {} slot {} {}; {} dropped into the world",
                    BridgeSupport.where(site, delegate), slot, reason, back);
        } else {
            BridgeSupport.LOGGER.error(
                    "Item bridge: foreign handler at {} slot {} {}; {} lost (no position to drop at)",
                    BridgeSupport.where(site, delegate), slot, reason, back);
        }
    }
}
