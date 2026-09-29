// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import net.minecraft.world.item.Item;
import com.hbm.backport.ItemInstance;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.ItemCompat;

public abstract class StackRemainderItem extends ItemCompat {

    protected StackRemainderItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public final @Nullable ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        return remainder(
                switch (instance) {
                    case ItemStack stack -> stack;
                    case ItemStackTemplate template -> template.create();
                    default -> throw new IllegalArgumentException(instance.getClass().getName());
                });
    }

    protected abstract @Nullable ItemStackTemplate remainder(ItemStack consumed);
}
