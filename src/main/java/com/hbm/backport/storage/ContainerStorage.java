// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * 26.x ContainerHelper.saveAllItems / loadAllItems, which take ValueOutput / ValueInput,
 * over the 1.21.1 CompoundTag versions. Same "Items" list with a byte "Slot" per entry,
 * so saves stay readable by either.
 */
public final class ContainerStorage {

    private ContainerStorage() {}

    public static void saveAllItems(ValueOutput output, NonNullList<ItemStack> items) {
        saveAllItems(output, items, true);
    }

    public static void saveAllItems(ValueOutput output, NonNullList<ItemStack> items, boolean alwaysPutTag) {
        ContainerHelper.saveAllItems(TagValueOutput.tagOf(output), items, alwaysPutTag, TagValueOutput.lookupOf(output));
    }

    public static void loadAllItems(ValueInput input, NonNullList<ItemStack> items) {
        ContainerHelper.loadAllItems(TagValueInput.tagOf(input), items, input.lookup());
    }
}
