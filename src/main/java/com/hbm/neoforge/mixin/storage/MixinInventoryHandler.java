// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.inventory.InventoryAlias;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.ItemInteriorBackend;
import com.hbm.inventory.ListSlotSource;
import com.hbm.neoforge.inventory.SophisticatedContentsStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import com.hbm.lib.neotransfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// backport: Sophisticated Core 1.21.1 (1.5.x) InventoryHandler is an ItemStackHandler (IItemHandler),
// not the NeoForge-26 transfer ItemStacksResourceHandler. The 26.x raw accessors map to their 1.21.1
// counterparts: getInternalStack -> getSlotStack (stacks.get(slot), bypassing the inventory partitioner),
// setStackInSlotInternal -> setSlotStack (stacks.set + slot tracker + onContentsChanged, which saves the
// persistent contents). unverified: that setSlotStack's change notification matches 26.x
// setStackInSlotInternal (26.x Sophisticated sources are not available here).
// backport: in 26.x the handler inherits the woven ListSlotSource members of ItemStacksResourceHandler
// (its "stacks" list, an external inventory owner, list-replacement hooks: StorageLayouts.ITEM_HANDLER
// and STACK_HANDLER). NeoForge 1.21.1's ItemStackHandler is not woven, so this mixin declares them over
// ItemStackHandler.stacks; the list-replacement hook is StorageLayouts.SOPH_INVENTORY (setSize, which
// deserializeNBT goes through). 26.x ContainerContents.InventoryData was both the record identity and
// the item interior's slot source (InteriorSlotSource); 1.21.1 has only the record tag every handler
// of a record shares (contentsNbt), so the identity comes from that tag, and a handler owned by an
// item interior backend delegates to the backend the way InteriorSlotSource does.
@Mixin(value = InventoryHandler.class, remap = false, priority = 500)
public abstract class MixinInventoryHandler extends ItemStackHandler
        implements ListSlotSource, InventoryAlias {
    @Shadow
    public abstract ItemStack getSlotStack(int slot);

    @Shadow
    public abstract boolean isInfinite(int slot);

    @Shadow
    public abstract void setSlotStack(int slot, ItemStack stack);

    @Shadow
    private boolean updateSlotNbt(int slot) {
        throw new AssertionError();
    }

    @Shadow
    public abstract void saveInventory();

    @Shadow
    public abstract void triggerOnChangeListeners(int slot);

    @Shadow @Final private CompoundTag contentsNbt;
    @Shadow private boolean persistent;
    @Unique private @Nullable Object hbm$inventoryOwner;

    @Override
    public Object inventoryIdentity() {
        return SophisticatedContentsStorage.inventoryIdentity(contentsNbt);
    }

    @Override
    public @Nullable Object inventoryOwner() {
        return hbm$inventoryOwner;
    }

    @Override
    public void inventoryOwner(@Nullable Object owner) {
        hbm$inventoryOwner = owner;
    }

    @Override
    public NonNullList<ItemStack> inventorySlotList() {
        return stacks;
    }

    @Override
    public void inventorySlotList(NonNullList<ItemStack> items) {
        stacks = items;
    }

    @Override
    public boolean directStoredMutation() {
        return false;
    }

    @Override
    public void prepareStoredSlot(int slot) {
        // backport: 26.x compared the persisted InventoryData stack with the live one and rewrote
        // the record when they differ. 1.21.1 persists per-slot NBT and updateSlotNbt is exactly
        // that compare-and-rewrite; saving and the change listeners follow it as in
        // onContentsChanged.
        if (persistent
                && Transaction.getLifecycle() == Transaction.Lifecycle.NONE
                && updateSlotNbt(slot)) {
            saveInventory();
            triggerOnChangeListeners(slot);
        }
    }

    @Override
    public int storedSlotCount() {
        // backport: ItemStacksResourceHandler.size() -> IItemHandler.getSlots()
        return getSlots();
    }

    @Override
    public ItemStack storedItem(int slot) {
        return isInfinite(slot) ? ItemStack.EMPTY : getSlotStack(slot);
    }

    @Override
    public boolean physicalSlot(int slot) {
        if (hbm$inventoryOwner instanceof ItemInteriorBackend backend)
            return backend.physicalSlot(this, slot);
        return slot >= 0 && slot < storedSlotCount() && !isInfinite(slot);
    }

    @Inject(method = "onFilterItemsChanged", at = @At("RETURN"))
    private void hbm$slotOwnershipChanged(CallbackInfo ci) {
        InventoryChanges.layoutChanged(this);
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (hbm$inventoryOwner instanceof ItemInteriorBackend backend)
            return backend.replaceStoredItem(this, slot, expected, replacement);
        // backport: the 26.x guard refused writes inside an open NeoForge transfer transaction; kept
        // against the tree's transfer shim (Sophisticated 1.21.1 itself has no transactions).
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || isInfinite(slot)
                || getSlotStack(slot) != expected) return false;
        setSlotStack(slot, replacement);
        return true;
    }

    @Override
    public void publishStoredChanges(int slot) {
        if (hbm$inventoryOwner instanceof ItemInteriorBackend backend)
            backend.publishStoredChanges(this, slot);
    }
}
