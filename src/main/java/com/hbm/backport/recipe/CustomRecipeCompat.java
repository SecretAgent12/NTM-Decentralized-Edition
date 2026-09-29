// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;

/**
 * 26.x CustomRecipe on 1.21.1's: no book category argument (misc), assemble without a
 * registry lookup, and no grid-size check (1.21.1 uses canCraftInDimensions only to
 * filter the recipe book, which never shows special recipes).
 */
public abstract class CustomRecipeCompat extends CustomRecipe {

    protected CustomRecipeCompat() {
        super(CraftingBookCategory.MISC);
    }

    protected CustomRecipeCompat(CraftingBookCategory category) {
        super(category);
    }

    public abstract ItemStack assemble(CraftingInput input);

    @Override
    public final ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return assemble(input);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }
}
