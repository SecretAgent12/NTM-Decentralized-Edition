// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.access.ItemAccess;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.SnapshotJournal;
import com.hbm.lib.neotransfer.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * {@link ItemAccess} over a free-standing container stack, used when a 1.21.1 caller asks one of the mod's
 * items for {@code IFluidHandlerItem}/{@code IEnergyStorage} (1.21.1 item capabilities have no context, so the
 * mod's {@code ItemAccess}-based handler is bound to this).
 *
 * <p>Changes are journaled; the current container is {@link #container()} ({@code IFluidHandlerItem#getContainer}).
 * On root commit, if the result is still the same item with the same count, the component changes are also
 * written into the ORIGINAL stack in place and the original instance becomes the container again - 1.21.1
 * component-backed handlers ({@code FluidHandlerItemStack}, {@code ComponentEnergyStorage}) mutate the stack they
 * were obtained from, and callers of item energy storages in particular rely on that. If the item or count changes
 * (e.g. an empty cell becomes a different filled item) only {@link #container()} reflects it, as in 1.21.1.
 *
 * <p><b>Concurrent change guard.</b> The original stack is live (it sits in some foreign inventory). Its state is
 * recorded when the first change of a root transaction is made; if the original was changed by someone else by
 * the time of write-back (item, count or components differ from what was read - e.g. a second access over the
 * same stack wrote back first, or the stack was split), the write-back is aborted and logged instead of
 * overwriting the newer state. {@link TransferBridge} shares one access per stack while it is
 * {@linkplain #isActive active}, so that case needs a foreign caller doing something unusual.
 */
final class ContainerItemAccess implements ItemAccess {

    private final ItemStack original;
    private ItemStack current;
    /** Copy of {@link #original} taken at the first change of the current root transaction; null = none. */
    private @Nullable ItemStack readState;

    private final SnapshotJournal<ItemStack> journal =
            new SnapshotJournal<>() {
                @Override
                protected ItemStack createSnapshot() {
                    return current; // never mutated while referenced by a snapshot: changes replace the stack
                }

                @Override
                protected void revertToSnapshot(ItemStack snapshot) {
                    current = snapshot;
                }

                @Override
                protected void onRootCommit(ItemStack originalState) {
                    writeBack();
                }
            };

    ContainerItemAccess(ItemStack stack) {
        this.original = stack;
        this.current = stack;
    }

    ItemStack container() {
        return current;
    }

    /** {@code true} while changes of a still-open transaction are journaled (lookups then share this access). */
    boolean isActive() {
        return journal.isInTransaction();
    }

    /** Whether the container is still the original item with the original count (components may differ). */
    boolean keepsItemAndCount() {
        return current == original
                || (!current.isEmpty()
                        && current.getItem() == original.getItem()
                        && current.getCount() == original.getCount());
    }

    private void beforeChange(TransactionContext transaction) {
        // First change of this root transaction: remember what the (live) original looked like.
        if (!journal.isInTransaction()) readState = original.copy();
        journal.updateSnapshots(transaction);
    }

    private void writeBack() {
        ItemStack read = readState;
        readState = null;
        if (current == original || current.isEmpty()) return;
        // Item or count changed: the result is only available through container(), as in 1.21.1.
        if (current.getItem() != original.getItem() || current.getCount() != original.getCount()) return;
        if (read != null && !ItemStack.matches(read, original)) {
            // Writing now would overwrite a change made by someone else since we read the stack (lost update =
            // duplication/deletion). The caller already got its result; this change is dropped instead.
            BridgeSupport.LOGGER.error(
                    "Item bridge: {} changed from {} while a transfer on it was pending; write-back of {} aborted",
                    original, read, current);
            current = original;
            return;
        }
        BridgeSupport.syncComponents(original, current);
        current = original;
    }

    @Override
    public ItemResource getResource() {
        return ItemResource.of(current);
    }

    @Override
    public int getAmount() {
        return current.isEmpty() ? 0 : current.getCount();
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;
        int max = resource.getMaxStackSize();
        if (current.isEmpty()) {
            int n = Math.min(amount, max);
            if (n <= 0) return 0;
            beforeChange(transaction);
            current = resource.toStack(n);
            return n;
        }
        if (!resource.matches(current)) return 0;
        int n = Math.min(amount, max - current.getCount());
        if (n <= 0) return 0;
        beforeChange(transaction);
        current = current.copyWithCount(current.getCount() + n);
        return n;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || current.isEmpty() || !resource.matches(current)) return 0;
        int n = Math.min(amount, current.getCount());
        beforeChange(transaction);
        current = n == current.getCount() ? ItemStack.EMPTY : current.copyWithCount(current.getCount() - n);
        return n;
    }
}
