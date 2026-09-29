// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes a transfer-API item handler as a 1.21.1 {@link IItemHandler}.
 *
 * <p>Every call runs in its own transaction (see {@link BridgeSupport#open(boolean)}): {@code simulate=true} aborts it,
 * {@code simulate=false} commits it. {@link #getStackInSlot} returns a freshly built stack (never the handler's
 * internal state), so callers that mutate it - against the IItemHandler contract - cannot corrupt the handler;
 * the flip side is one allocation per call.
 *
 * <p>Deliberately NOT {@code IItemHandlerModifiable}: {@code setStackInSlot} is an unconditional overwrite with no
 * transfer-API equivalent (extract-all + insert can fail half way on filtered/output slots and would not be
 * atomic outside a transaction), so offering it would silently lose or duplicate items.
 */
final class ExportedItemHandler implements IItemHandler {

    final ResourceHandler<ItemResource> handler;

    ExportedItemHandler(ResourceHandler<ItemResource> handler) {
        this.handler = handler;
    }

    @Override
    public int getSlots() {
        return handler.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemResource resource = handler.getResource(slot);
        int amount = handler.getAmountAsInt(slot);
        return resource.isEmpty() || amount <= 0 ? ItemStack.EMPTY : resource.toStack(amount);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int inserted;
        try (Transaction tx = BridgeSupport.open(!simulate)) {
            if (tx == null) return stack;
            inserted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
        }
        if (inserted <= 0) return stack;
        return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        // 1.21.1 callers expect a valid stack back: never more than one max-size stack per call.
        int request = Math.min(amount, resource.getMaxStackSize());
        int extracted;
        try (Transaction tx = BridgeSupport.open(!simulate)) {
            if (tx == null) return ItemStack.EMPTY;
            extracted = handler.extract(slot, resource, request, tx);
            if (!simulate) tx.commit();
        }
        return extracted > 0 ? resource.toStack(extracted) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return handler.getCapacityAsInt(slot, ItemResource.EMPTY);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !stack.isEmpty() && handler.isValid(slot, ItemResource.of(stack));
    }
}
