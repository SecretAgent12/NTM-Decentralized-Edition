// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

// backport: 26.x partial component predicates (DataComponentPredicate) are 1.21.1
// ItemSubPredicates.
public interface ComponentStatePredicate extends ItemSubPredicate {
    boolean claims(DataComponentType<?> component, ItemStack stack);
}
