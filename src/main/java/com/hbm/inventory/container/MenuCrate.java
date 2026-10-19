// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.blockentity.machine.storage.BlockEntityCrate;
import com.hbm.blockentity.machine.storage.CrateType;
import com.hbm.platform.SophisticatedCoreNesting;
import com.hbm.tags.HbmItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import com.hbm.backport.transfer.Capabilities;
import com.hbm.lib.neotransfer.access.ItemAccess;

public class MenuCrate extends NtmContainerMenu {

    private final CrateType type;

    public MenuCrate(int containerId, Inventory playerInv, CrateType type) {
        this(containerId, playerInv, type, new SimpleContainer(type.slots));
    }

    public MenuCrate(int containerId, Inventory playerInv, BlockEntityCrate crate) {
        this(containerId, playerInv, crate.crateType, crate);
    }

    public MenuCrate(int containerId, Inventory playerInv, CrateType type, Container container) {
        super(ModMenus.crateMenuType(type).get(), containerId, container);
        checkContainerSize(container, type.slots);
        this.type = type;

        for (int row = 0; row < type.rows; row++) {
            for (int col = 0; col < type.columns; col++) {
                int slot = col + row * type.columns;
                addSlot(
                        new ShulkerBoxSlot(
                                container, slot, type.crateX + col * 18, type.crateY + row * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return (stack.canFitInsideContainerItems()
                                                && !stack.is(HbmItemTags.NO_CONTAINER_NESTING)
                                                && !com.hbm.backport.ContainerNesting.holdsItems(stack) /* backport-fix: BF-102 */
                                                && !stack.has(DataComponents.BUNDLE_CONTENTS)
                                                && (stack.isEmpty()
                                                        || stack.getCapability(
                                                                        Capabilities.Item.ITEM,
                                                                        ItemAccess.forStack(stack))
                                                                == null)
                                                && (!ModList.get().isLoaded("sophisticatedcore")
                                                        || !SophisticatedCoreNesting.hasStorage(
                                                                stack)))
                                        && super.mayPlace(stack);
                            }
                        });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                playerInv,
                                col + row * 9 + 9,
                                type.playerInventoryX + col * 18,
                                type.playerInventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, type.playerInventoryX + col * 18, type.hotbarY));
        }
        if (!playerInv.player.level().isClientSide()) returnRefused(playerInv.player, type.slots);
        container.startOpen(playerInv.player);
    }

    /**
     * backport-fix: BF-103 since NTM: NEXT 1.0.3 crate slots refuse nested storage (shulker boxes, bundles,
     * backpacks, filled chests, NTM's own crates), but crates filled before that (DE 1.0.0-beta.10 and earlier)
     * can still hold such items. Inventory sorters that put a refused item back slot by slot (Inventory Sorter
     * clears a slot first, then asks mayPlace, and keeps trying the next slots) then wiped everything sorted after
     * it. Whatever the slots refuse is handed to the player opening the crate (dropped at their feet if their
     * inventory is full), so a crate's contents always pass its own slot rules.
     */
    private void returnRefused(Player player, int crateSlots) {
        boolean returned = false;
        for (int i = 0; i < crateSlots; i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || slot.mayPlace(stack)) continue;
            slot.set(ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(stack);
            returned = true;
        }
        if (returned) {
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.hbm.crate.refused_returned",
                            "Items this crate can't hold any more were moved to your inventory"),
                    false);
        }
    }

    public CrateType getCrateType() {
        return type;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container().stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    int crateSlots = type.slots;
                    int invEnd = slots.size();
                    if (index < crateSlots)
                        return moveItemStackToFiltered(stack, crateSlots, invEnd, true);
                    return moveItemStackToFiltered(stack, 0, crateSlots, false);
                });
    }
}
