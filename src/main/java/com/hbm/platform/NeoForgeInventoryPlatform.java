// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.inventory.SlotSource;
import net.minecraft.world.inventory.AbstractContainerMenu;

// backport: DE has no AE2 integration, so no AE2 item contents, block item layouts or menu
// inventories are registered
public final class NeoForgeInventoryPlatform {
    public static void registerContents() {}

    public static SlotSource menuInventory(AbstractContainerMenu menu) {
        return null;
    }
}
