// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.inventory.StorageLayouts;
import org.spongepowered.asm.mixin.Mixin;

// backport: only the targets that exist for DE on 1.21.1 (no ShelfBlockEntity; no Tech Reborn, AE2,
// or Sophisticated ContainerContents data classes), matching StorageLayouts
@Mixin(
        targets = {
            StorageLayouts.BASE, StorageLayouts.CHEST, StorageLayouts.BARREL, StorageLayouts.HOPPER,
            StorageLayouts.DISPENSER, StorageLayouts.SHULKER, StorageLayouts.BREWING,
                    StorageLayouts.CRAFTER,
            StorageLayouts.CAMPFIRE, StorageLayouts.BOOKSHELF, StorageLayouts.LECTERN,
            StorageLayouts.LECTERN_MENU, StorageLayouts.BRUSHABLE, StorageLayouts.JUKEBOX,
                    StorageLayouts.POT,
            StorageLayouts.VAULT, StorageLayouts.CRAFTING, StorageLayouts.TOM_MATRIX,
                    StorageLayouts.COMPACT,
            StorageLayouts.DRUM, StorageLayouts.STACK_HANDLER
        },
        priority = 500,
        remap = false)
public abstract class MixinStorageTargets {}
