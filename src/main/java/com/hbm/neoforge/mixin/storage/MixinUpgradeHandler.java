// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.inventory.InventoryAlias;
import com.hbm.inventory.ItemInteriorBackend;
import com.hbm.inventory.ListSlotSource;
import com.hbm.lib.neotransfer.transaction.Transaction;
import com.hbm.neoforge.inventory.SophisticatedContentsStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

// backport: in 26.x UpgradeHandler extends ItemStacksResourceHandler and inherits its woven
// ListSlotSource members (StorageLayouts.ITEM_HANDLER, replaceStoredItem from
// MixinItemStacksResourceHandler). Sophisticated Core 1.21.1's UpgradeHandler is a NeoForge
// ItemStackHandler, so the same members are declared here over ItemStackHandler.stacks (the
// list-replacement hook is StorageLayouts.UPGRADES). The identity 26.x took from
// ContainerContents.UpgradeData comes from the shared record tag (contentsNbt), and a handler owned
// by an item interior backend delegates to it as InteriorSlotSource does (see MixinInventoryHandler).
@Mixin(value = UpgradeHandler.class, remap = false, priority = 500)
public abstract class MixinUpgradeHandler extends ItemStackHandler
        implements ListSlotSource, InventoryAlias {
    @Shadow @Final private CompoundTag contentsNbt;
    @Unique private @Nullable Object hbm$inventoryOwner;

    @Override
    public Object inventoryIdentity() {
        return SophisticatedContentsStorage.upgradesIdentity(contentsNbt);
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
    public boolean physicalSlot(int slot) {
        if (hbm$inventoryOwner instanceof ItemInteriorBackend backend)
            return backend.physicalSlot(this, slot);
        return slot >= 0 && slot < storedSlotCount();
    }

    @Override
    public boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement) {
        if (hbm$inventoryOwner instanceof ItemInteriorBackend backend)
            return backend.replaceStoredItem(this, slot, expected, replacement);
        // backport: MixinItemStacksResourceHandler.replaceStoredItem; set(slot, resource, count)
        // -> setStackInSlot (UpgradeHandler's override keeps its upgrade wrappers in step)
        if (Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                || stacks.get(slot) != expected) return false;
        setStackInSlot(slot, replacement);
        return ItemStack.matches(stacks.get(slot), replacement);
    }

    @Override
    public void publishStoredChanges(int slot) {
        if (hbm$inventoryOwner instanceof ItemInteriorBackend backend)
            backend.publishStoredChanges(this, slot);
    }
}
