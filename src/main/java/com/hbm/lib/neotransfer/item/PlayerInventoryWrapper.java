/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer.item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import com.hbm.lib.neotransfer.CombinedResourceHandler;
import com.hbm.lib.neotransfer.RangedResourceHandler;
import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.ResourceHandlerUtil;
import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.transaction.SnapshotJournal;
import com.hbm.lib.neotransfer.transaction.TransactionContext;

/**
 * An implementation of {@code ResourceHandler<ItemResource>} for the {@link Inventory} of a {@link Player}.
 *
 * @see VanillaContainerWrapper
 * @see WorldlyContainerWrapper
 */
// TODO: do we want to "animate" client-side inserted items with setPopTime(POP_TIME_DURATION)?
public final class PlayerInventoryWrapper extends VanillaContainerWrapper {
    /**
     * Gets the inventory wrapper for a {@link Player}.
     */
    public static PlayerInventoryWrapper of(Player player) {
        return of(player.getInventory());
    }

    /**
     * Gets the inventory wrapper for a player's {@link Inventory}.
     */
    public static PlayerInventoryWrapper of(Inventory inventory) {
        return (PlayerInventoryWrapper) VanillaContainerWrapper.of(inventory);
    }

    private final DroppedItems droppedItems = new DroppedItems();
    private final Inventory inventory;

    PlayerInventoryWrapper(Inventory inventory) {
        super(inventory);
        this.inventory = inventory;
    }

    @Override
    void resize() {
        // We currently limit the player wrapper to main + armor + offhand. This can be changed later if needed.
        // backport: 26.x uses Inventory.SLOT_BODY_ARMOR (= offhand + 1); 1.21.1 has no body armor slot in the player inventory.
        size = Inventory.SLOT_OFFHAND + 1;
        while (slotWrappers.size() < size) {
            int index = slotWrappers.size();
            if (Inventory.INVENTORY_SIZE <= index && index < Inventory.SLOT_OFFHAND) {
                // backport: 26.x uses Inventory.EQUIPMENT_SLOT_MAPPING; in 1.21.1 the armor slots are laid out by EquipmentSlot#getIndex.
                var equipmentSlot = armorSlotForIndex(index - Inventory.INVENTORY_SIZE);
                slotWrappers.add(new ArmorSlotWrapper(index, equipmentSlot));
            } else {
                slotWrappers.add(new SlotWrapper(index));
            }
        }
    }

    private static EquipmentSlot armorSlotForIndex(int armorIndex) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && slot.getIndex() == armorIndex) {
                return slot;
            }
        }
        throw new IllegalStateException("No armor equipment slot for armor index " + armorIndex);
    }

    @Override
    void onRootCommit() {
        super.onRootCommit();
        // This sends a ClientboundContainerSetSlotPacket for each changed slot,
        // which seems to be a good thing to do based on vanilla's Inventory#placeItemBackInInventory
        if (!inventory.player.level().isClientSide()) {
            inventory.player.containerMenu.broadcastChanges();
        }
    }

    /**
     * Retrieves a wrapper for a specific slot.
     */
    public ResourceHandler<ItemResource> getSlot(int slot) {
        return getSlotWrapper(slot);
    }

    /**
     * Retrieves a wrapper for the slot corresponding to the current main hand.
     */
    public ResourceHandler<ItemResource> getMainHandSlot() {
        // backport: 1.21.1 exposes the selected slot as the Inventory#selected field instead of getSelectedSlot()
        if (Inventory.isHotbarSlot(inventory.selected)) {
            return getSlot(inventory.selected);
        } else {
            throw new RuntimeException("Unexpected player selected slot: " + inventory.selected);
        }
    }

    /**
     * Retrieves a wrapper for the slot corresponding to the given hand.
     */
    public ResourceHandler<ItemResource> getHandSlot(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> getMainHandSlot();
            case OFF_HAND -> getSlot(Inventory.SLOT_OFFHAND);
        };
    }

    /**
     * Retrieves a wrapper around both hand slots.
     */
    public ResourceHandler<ItemResource> getHandSlots() {
        return new CombinedResourceHandler<>(getMainHandSlot(), getHandSlot(InteractionHand.OFF_HAND));
    }

    /**
     * Retrieves a wrapper around the main slots only.
     */
    public ResourceHandler<ItemResource> getMainSlots() {
        return RangedResourceHandler.of(this, 0, Inventory.INVENTORY_SIZE);
    }

    /**
     * Retrieves a wrapper around a single armor slot.
     */
    public ResourceHandler<ItemResource> getArmorSlot(EquipmentSlot slot) {
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            throw new IllegalArgumentException("EquipmentSlot is not an armor slot: " + slot);
        }
        return getSlot(slot.getIndex(Inventory.INVENTORY_SIZE));
    }

    /**
     * Retrieves a wrapper around all 4 armor slots.
     */
    public ResourceHandler<ItemResource> getArmorSlots() {
        return RangedResourceHandler.of(this, Inventory.INVENTORY_SIZE, Inventory.SLOT_OFFHAND);
    }

    /**
     * Inserts items into this player inventory, trying to place items
     * following the logic of {@link Inventory#placeItemBackInInventory}.
     */
    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        int inserted = 0;

        // Stack into the main stack first and the offhand stack second.
        for (InteractionHand hand : InteractionHand.values()) {
            var handSlot = getHandSlot(hand);

            if (handSlot.getResource(0).equals(resource)) {
                inserted += handSlot.insert(resource, amount - inserted, transaction);
                if (inserted == amount) {
                    return inserted;
                }
            }
        }

        // Otherwise insert into the main slots, stacking first.
        inserted += ResourceHandlerUtil.insertStacking(getMainSlots(), resource, amount - inserted, transaction);

        return inserted;
    }

    /**
     * Transactional version of {@link Inventory#placeItemBackInInventory}:
     * tries to insert as much as possible into the player inventory, and drops the remainder.
     */
    public void placeItemBackInInventory(ItemResource resource, int amount, TransactionContext transactionContext) {
        int inserted = insert(resource, amount, transactionContext);
        if (inserted < amount) {
            // If we couldn't insert all of it, drop the remainder.
            drop(resource, amount - inserted, false, false, transactionContext);
        }
    }

    /**
     * Transactionally drops an item in the world.
     */
    public void drop(ItemResource resource, int amount, boolean dropAround, boolean includeThrowerName, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return;

        // Drop in the world on the server side (will be synced by the game with the client).
        // Dropping items is server-side only because it involves randomness.
        if (!inventory.player.level().isClientSide()) {
            droppedItems.addDrop(resource, amount, dropAround, includeThrowerName, transaction);
        }
    }

    @Override
    public String toString() {
        return "PlayerInventoryWrapper{player=%s}".formatted(inventory.player);
    }

    private class DroppedItems extends SnapshotJournal<Integer> {
        final Deque<DropInfo> entries = new ArrayDeque<>();

        void addDrop(ItemResource resource, int amount, boolean dropAround, boolean includeThrowerName, TransactionContext transaction) {
            updateSnapshots(transaction);
            entries.add(new DropInfo(resource, amount, dropAround, includeThrowerName));
        }

        @Override
        protected Integer createSnapshot() {
            return entries.size();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            // effectively cancel dropping the stacks
            int previousSize = snapshot;

            while (entries.size() > previousSize) {
                entries.removeLast();
            }
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            // actually drop the stacks
            // process elements of the queue one by one to avoid a CME if dropping the entity triggers more additions to the queue
            while (!entries.isEmpty()) {
                DropInfo dropInfo = entries.removeFirst();
                int remainder = dropInfo.amount;

                int maxStackSize = dropInfo.resource.getMaxStackSize();
                while (remainder > 0) {
                    int dropped = Math.min(maxStackSize, remainder);
                    // This takes care of firing ItemTossEvent + dropping the entity if the event is not canceled
                    tossItem(inventory.player, dropInfo.resource.toStack(dropped), dropInfo.dropAround, dropInfo.includeThrowerName);
                    remainder -= dropped;
                }
            }
        }

        // backport: 1.21.1's CommonHooks.onPlayerTossEvent(Player, ItemStack, boolean) has no dropAround parameter
        // (always drops in front of the player), so its logic is reproduced here with dropAround passed through.
        private static void tossItem(Player player, ItemStack stack, boolean dropAround, boolean includeThrowerName) {
            player.captureDrops(new ArrayList<>());
            ItemEntity entity = player.drop(stack, dropAround, includeThrowerName);
            player.captureDrops(null);
            if (entity == null) {
                return;
            }
            ItemTossEvent event = new ItemTossEvent(entity, player);
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                return;
            }
            if (!player.level().isClientSide()) {
                player.level().addFreshEntity(event.getEntity());
            }
        }

        private record DropInfo(ItemResource resource, int amount, boolean dropAround, boolean includeThrowerName) {}
    }

    /**
     * Specialized slot wrapper for one of the armor slots.
     * Limits size to 1, disallows insertion of non-equippable items, and extraction of cursed items for non-creative players.
     */
    private class ArmorSlotWrapper extends SlotWrapper {
        private final EquipmentSlot slot;

        ArmorSlotWrapper(int index, EquipmentSlot slot) {
            super(index);
            this.slot = slot;
        }

        @Override
        protected boolean isValid(ItemResource resource) {
            return resource.toStack().canEquip(slot, inventory.player) && super.isValid(resource);
        }

        @Override
        protected int getCapacity(ItemResource resource) {
            // backport: EquipmentSlot#countLimit is private in 1.21.1; it is 1 for every humanoid armor slot.
            return 1;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            // Prevent extraction of items with the curse of binding for non-creative players.
            if (!inventory.player.isCreative() && EnchantmentHelper.has(resource.toStack(), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                return 0;
            }
            return super.extract(index, resource, amount, transaction);
        }
    }
}
