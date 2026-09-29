// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 26.x ItemInstance: what an ItemStack and an ItemStackTemplate have in common.
 * Injected into ItemStack (interface-injection.json for compiling, MixinItemStack
 * at runtime); the defaults are ItemStack's side, ItemStackTemplate overrides them.
 */
public interface ItemInstance extends DataComponentHolder {

    default int count() {
        return ((ItemStack) (Object) this).getCount();
    }

    default Holder<Item> typeHolder() {
        return ((ItemStack) (Object) this).getItemHolder();
    }
}
