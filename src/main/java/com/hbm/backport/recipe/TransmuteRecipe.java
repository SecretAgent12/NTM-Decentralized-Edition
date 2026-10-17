// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.recipe;

import com.hbm.backport.ItemStackTemplate;
import net.minecraft.world.item.ItemStack;

/**
 * 26.x TransmuteRecipe.createWithOriginalComponents: the template's item and count carrying
 * the input stack's components, with the template's own components on top (as the 1.21.2+
 * TransmuteResult does: input.transmuteCopy, then the result's component patch). 1.21.1 has
 * no TransmuteRecipe; only this helper is used.
 */
public final class TransmuteRecipe {

    private TransmuteRecipe() {}

    public static ItemStack createWithOriginalComponents(ItemStackTemplate target, ItemStack input) {
        ItemStack stack = input.transmuteCopy(target.getItem(), target.count());
        stack.applyComponents(target.components());
        return stack;
    }
}
