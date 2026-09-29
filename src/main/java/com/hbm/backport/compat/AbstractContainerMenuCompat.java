// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.compat;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * AbstractContainerMenu with the player-inventory slot helpers 1.21.2 added
 * (addStandardInventorySlots and the two it is built from). Same layout as vanilla's:
 * three rows of 18 px from (x, y), and the hotbar 4 px below them.
 */
public abstract class AbstractContainerMenuCompat extends AbstractContainerMenu {

    protected AbstractContainerMenuCompat(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    protected void addInventoryHotbarSlots(Container container, int x, int y) {
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(container, i, x + i * 18, y));
        }
    }

    protected void addInventoryExtendedSlots(Container container, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
    }

    protected void addStandardInventorySlots(Container container, int x, int y) {
        addInventoryExtendedSlots(container, x, y);
        addInventoryHotbarSlots(container, x, y + 3 * 18 + 4);
    }
}
