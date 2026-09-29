/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer.item;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.hbm.lib.neotransfer.IndexModifier;
import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.StacksResourceHandler;
import com.hbm.lib.neotransfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Base implementation for a {@link Slot} backed by a {@code ResourceHandler<ItemResource>}.
 * Requires the handler to expose a {@linkplain IndexModifier direct mutation function},
 * such as {@link StacksResourceHandler#set}.
 */
// backport: 26.x extends net.neoforged.neoforge.world.inventory.StackCopySlot(index, x, y). 1.21.1's
// net.neoforged.neoforge.items.StackCopySlot only has an (x, y) constructor that hardcodes slot index 0,
// so the equivalent stack-copy logic (getItem/set/setChanged/remove) is implemented directly on top of Slot here.
public class ResourceHandlerSlot extends Slot {
    private static final Container EMPTY_CONTAINER = new SimpleContainer(0);

    private final ResourceHandler<ItemResource> handler;
    private final IndexModifier<ItemResource> slotModifier;
    private @Nullable ItemStack cachedReturnedStack = null;

    public ResourceHandlerSlot(ResourceHandler<ItemResource> handler, IndexModifier<ItemResource> slotModifier, int handlerSlot, int xPosition, int yPosition) {
        super(EMPTY_CONTAINER, handlerSlot, xPosition, yPosition);
        this.handler = handler;
        this.slotModifier = slotModifier;
    }

    // backport: stack-copy slot logic (from StackCopySlot). Vanilla code modifies the stack returned by getItem()
    // directly and then calls setChanged(), so the returned copy is cached and written back in setChanged().
    @Override
    public final ItemStack getItem() {
        return cachedReturnedStack = getStackCopy();
    }

    @Override
    public final void set(ItemStack stack) {
        setStackCopy(stack);
        cachedReturnedStack = stack;
    }

    @Override
    public final void setChanged() {
        if (cachedReturnedStack != null) {
            set(cachedReturnedStack);
        }
    }

    @Override
    public final ItemStack remove(int amount) {
        ItemStack stack = getStackCopy().copy();
        ItemStack ret = stack.split(amount);
        set(stack);
        cachedReturnedStack = null;
        return ret;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        // Use isValid as a reasonable estimate.
        // We can't try to insert as we don't want to check the current contents to allow swapping.
        // This method is left for mods to override if this is not sufficient.
        return handler.isValid(this.getSlotIndex(), ItemResource.of(stack));
    }

    protected ItemStack getStackCopy() {
        return handler.getResource(this.getSlotIndex()).toStack(handler.getAmountAsInt(this.getSlotIndex()));
    }

    protected void setStackCopy(ItemStack stack) {
        slotModifier.set(this.getSlotIndex(), ItemResource.of(stack), stack.getCount());
    }

    @Override
    public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {}

    @Override
    public int getMaxStackSize() {
        return handler.getCapacityAsInt(this.getSlotIndex(), ItemResource.EMPTY);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return handler.getCapacityAsInt(this.getSlotIndex(), ItemResource.of(stack));
    }

    @Override
    public boolean mayPickup(Player player) {
        var resource = handler.getResource(this.getSlotIndex());
        if (resource.isEmpty()) {
            return false;
        }
        try (var tx = Transaction.openRoot()) {
            // Simulated extraction
            return handler.extract(this.getSlotIndex(), resource, 1, tx) == 1;
        }
    }

    public ResourceHandler<ItemResource> getResourceHandler() {
        return handler;
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return other instanceof ResourceHandlerSlot rhs && rhs.handler == this.handler;
    }
}
