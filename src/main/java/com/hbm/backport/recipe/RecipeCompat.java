// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * A 26.x-shaped Recipe on the 1.21.1 interface.
 *
 * 26.x recipes assemble without a registry lookup, have no fixed result item and no
 * grid-size check (placement is described by PlacementInfo instead), and name their
 * group group(). The 1.21.1 methods are implemented here in terms of those:
 * getResultItem is empty (26.x has nothing to return there; all recipes that use this
 * are isSpecial, so the vanilla recipe book never shows them), canCraftInDimensions is
 * true (none of them are crafting-grid recipes).
 */
public interface RecipeCompat<T extends RecipeInput> extends Recipe<T> {

    ItemStack assemble(T input);

    @Override
    default ItemStack assemble(T input, HolderLookup.Provider registries) {
        return assemble(input);
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    default ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    default String getGroup() {
        return group();
    }

    default String group() {
        return "";
    }

    /** 26.x recipe-book placement; nothing in 1.21.1 reads it. */
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    /** 26.x recipe-book category; nothing in 1.21.1 reads it. */
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategory.UNUSED;
    }
}
