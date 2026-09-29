// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.access.ItemAccess;
import com.hbm.lib.neotransfer.energy.EnergyHandler;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a foreign item's 1.21.1 {@link IEnergyStorage} as the mod's {@code ItemAccess}-bound energy handler.
 * Same technique as {@link ImportedFluidItemHandler}: execute on a single-item copy (1.21.1 item energy storages
 * mutate the stack they came from), then exchange the accessed items for the changed copy through the journaled
 * {@link ItemAccess}. Stacks of N items are charged/discharged evenly. Items whose energy does not live in the
 * stack (creative/infinite, network-linked) cannot be made transactional and are refused.
 *
 * <p>Stateless over the {@link ItemAccess} (every call re-reads it and changes it only through its journal), so
 * several wrappers over the same access or the same slot in one transaction stay consistent; no per-slot cache is
 * needed (same for {@link ImportedFluidItemHandler}).
 */
final class ImportedEnergyItemHandler implements EnergyHandler {

    private final ItemAccess access;

    ImportedEnergyItemHandler(ItemAccess access) {
        this.access = access;
    }

    static @Nullable IEnergyStorage storageOf(ItemStack single) {
        return single.isEmpty() ? null : single.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
    }

    private @Nullable IEnergyStorage current() {
        ItemResource resource = access.getResource();
        return resource.isEmpty() ? null : storageOf(resource.toStack(1));
    }

    @Override
    public long getAmountAsLong() {
        IEnergyStorage s = current();
        return s == null ? 0 : (long) Math.max(0, s.getEnergyStored()) * access.getAmount();
    }

    @Override
    public long getCapacityAsLong() {
        IEnergyStorage s = current();
        return s == null ? 0 : (long) Math.max(0, s.getMaxEnergyStored()) * access.getAmount();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        return transfer(amount, transaction, true);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return transfer(amount, transaction, false);
    }

    private int transfer(int amount, TransactionContext transaction, boolean insert) {
        TransferPreconditions.checkNonNegative(amount);
        int count = access.getAmount();
        if (count <= 0 || amount < count) return 0;
        int perItem = amount / count;
        ItemResource before = access.getResource();
        if (before.isEmpty()) return 0;
        ItemStack copy = before.toStack(1);
        IEnergyStorage s = storageOf(copy);
        if (s == null) return 0;
        // Simulate first: for items whose energy lives outside the stack, the EXECUTE below really moves energy in
        // that external storage and has to be undone; skipping hopeless attempts keeps the mod's frequent probes
        // (aborted transactions) from churning it.
        if ((insert ? s.receiveEnergy(perItem, true) : s.extractEnergy(perItem, true)) <= 0) return 0;
        int moved = insert ? s.receiveEnergy(perItem, false) : s.extractEnergy(perItem, false);
        if (moved <= 0) return 0;
        if (moved > perItem) {
            // Over-report (contract violation): the copy's state matches no amount we could report. Refuse.
            BridgeSupport.LOGGER.error(
                    "Energy item bridge: {} reported moving {} FE when {} FE were requested; refused",
                    before, moved, perItem);
            undo(s, before, copy, moved, insert);
            return 0;
        }
        if (before.matches(copy)) {
            // State is not in the stack: undo on the copy (harmless for creative items) and refuse.
            undo(s, before, copy, moved, insert);
            return 0;
        }
        return moved * BridgeSupport.exchange(access, before, count, copy, transaction);
    }

    /**
     * Reverts an EXECUTE on the throw-away copy when the energy is not stored in the stack (the copy is unchanged,
     * so the execute hit shared/external storage). The inverse call can fall short (e.g. an insert-only network);
     * that is logged as an error - unverified whether any item in the modpack behaves like that.
     */
    private static void undo(IEnergyStorage s, ItemResource before, ItemStack copy, int moved, boolean wasInsert) {
        if (!before.matches(copy)) return; // state is in the copy: discarding it is the undo
        int undone = wasInsert ? s.extractEnergy(moved, false) : s.receiveEnergy(moved, false);
        if (undone != moved) {
            BridgeSupport.LOGGER.error(
                    "Energy item bridge: could not undo a probe on {} (external storage): {} of {} FE reverted",
                    before, undone, moved);
        }
    }
}
