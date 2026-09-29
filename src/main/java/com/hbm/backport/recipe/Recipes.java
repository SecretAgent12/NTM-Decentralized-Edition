// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** 26.x recipe calls that take different arguments in 1.21.1. */
public final class Recipes {

    private Recipes() {}

    /**
     * 26.x Recipe.assemble(input). 1.21.1 also wants a registry lookup: the running
     * server's when there is one (vanilla recipes use it only to copy their result).
     */
    public static <T extends RecipeInput> ItemStack assemble(Recipe<T> recipe, T input) {
        return recipe.assemble(input, registries());
    }

    public static HolderLookup.Provider registries() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
    }

    /** 26.x CraftingRecipe.defaultCraftingReminder: each slot's crafting remainder. */
    public static net.minecraft.core.NonNullList<ItemStack> defaultCraftingReminder(
            net.minecraft.world.item.crafting.CraftingInput input) {
        net.minecraft.core.NonNullList<ItemStack> out =
                net.minecraft.core.NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < out.size(); i++) {
            ItemStack item = input.getItem(i);
            if (item.hasCraftingRemainingItem()) out.set(i, item.getCraftingRemainingItem());
        }
        return out;
    }

    /** 26.x SingleItemRecipe.input(): the one ingredient of a cooking recipe. */
    public static Ingredient input(AbstractCookingRecipe recipe) {
        return recipe.getIngredients().get(0);
    }
}
