// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.backport.IInventorySlotBinding;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Backport: the field accesses were inline bytecode (tenon-asm) in ntm-next on
 * fields MixinPatchedDataComponentMap adds; rewritten as the plain Java they
 * encode through the IInventorySlotBinding duck interface the mixin implements.
 */
public final class InventorySlotBinding {
    private InventorySlotBinding() {}

    public static void bind(ItemStack stack, @Nullable Object source, int slot) {
        if (stack == ItemStack.EMPTY) return;
        PatchedDataComponentMap components = stack.components;
        ((IInventorySlotBinding) (Object) components).hbm$setInventorySource(source);
        ((IInventorySlotBinding) (Object) components).hbm$setInventorySlot(slot);
    }

    public static void unbind(ItemStack stack, Object source) {
        if (stack == ItemStack.EMPTY) return;
        PatchedDataComponentMap components = stack.components;
        if (source(components) == source) bind(stack, null, 0);
    }

    public static void unbind(ItemStack stack, Object source, int slot) {
        if (stack == ItemStack.EMPTY) return;
        PatchedDataComponentMap components = stack.components;
        if (source(components) == source && slot(components) == slot) bind(stack, null, 0);
    }

    public static void changed(ItemStack stack) {
        changed(stack.components);
    }

    public static boolean isBound(ItemStack stack) {
        return source(stack.components) != null;
    }

    public static void changed(PatchedDataComponentMap components) {
        Object source = source(components);
        if (source == null) return;
        InventoryChanges.slotChanged(source, slot(components));
    }

    private static int slot(PatchedDataComponentMap components) {
        return ((IInventorySlotBinding) (Object) components).hbm$inventorySlot();
    }

    private static Object source(PatchedDataComponentMap components) {
        return ((IInventorySlotBinding) (Object) components).hbm$inventorySource();
    }
}
