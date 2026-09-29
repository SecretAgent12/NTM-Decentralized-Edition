// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.interfaces.StoredItemSlots;
import com.hbm.lib.neotransfer.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

// backport: Sophisticated Core 1.21.1 (1.5.x) InventoryHandler is an ItemStackHandler (IItemHandler),
// not the NeoForge-26 transfer ItemStacksResourceHandler. The 26.x raw accessors map to their 1.21.1
// counterparts: getInternalStack -> getSlotStack (stacks.get(slot), bypassing the inventory partitioner),
// setStackInSlotInternal -> setSlotStack (stacks.set + slot tracker + onContentsChanged, which saves the
// persistent contents). unverified: that setSlotStack's change notification matches 26.x
// setStackInSlotInternal (26.x Sophisticated sources are not available here).
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class MixinSophisticatedInventory implements StoredItemSlots {
    @Shadow
    public abstract ItemStack getSlotStack(int slot);

    @Shadow
    public abstract boolean isInfinite(int slot);

    @Shadow
    public abstract void setSlotStack(int slot, ItemStack stack);

    @Override
    public int storedSlotCount() {
        // backport: ItemStacksResourceHandler.size() -> IItemHandler.getSlots()
        return ((IItemHandler) (Object) this).getSlots();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return isInfinite(slot) ? ItemStack.EMPTY : getSlotStack(slot);
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        // backport: the 26.x guard refused writes inside an open NeoForge transfer transaction; kept
        // against the tree's transfer shim (Sophisticated 1.21.1 itself has no transactions).
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || isInfinite(slot)
                || getSlotStack(slot) != expected) return false;
        setSlotStack(slot, replacement);
        return true;
    }
}
