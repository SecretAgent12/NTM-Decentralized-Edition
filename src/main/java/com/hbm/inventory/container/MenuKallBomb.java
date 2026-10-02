// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotComponent;
import com.hbm.tileentity.bomb.BlockEntityKallBomb;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MenuKallBomb extends NtmContainerMenu {

    private static final int MACHINE_SLOTS = BlockEntityKallBomb.SLOT_COUNT;

    public MenuKallBomb(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MACHINE_SLOTS));
    }

    public MenuKallBomb(int containerId, Inventory playerInv, BlockEntityKallBomb bomb) {
        this(containerId, playerInv, (Container) bomb);
    }

    private MenuKallBomb(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.KALL_BOMB.get(), containerId, container);
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new SlotComponent(container, BlockEntityKallBomb.SLOT_IGNITER, 26, 35));
        addSlot(new SlotComponent(container, BlockEntityKallBomb.SLOT_LENS_1, 8, 17));
        addSlot(new SlotComponent(container, BlockEntityKallBomb.SLOT_LENS_2, 44, 17));
        addSlot(new SlotComponent(container, BlockEntityKallBomb.SLOT_LENS_3, 8, 53));
        addSlot(new SlotComponent(container, BlockEntityKallBomb.SLOT_LENS_4, 44, 53));
        addSlot(new SlotComponent(container, BlockEntityKallBomb.SLOT_WASTE, 98, 35));

        addStandardInventorySlots(playerInv, 8, 84);
    }

    public ItemStack part(int slot) {
        return container().getItem(slot);
    }

    public boolean isReady() {
        return BlockEntityKallBomb.isReady(container());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveFilteredUnsorted(player, index, MACHINE_SLOTS);
    }
}
