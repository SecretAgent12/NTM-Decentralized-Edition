// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jei;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import java.util.List;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * backport-fix: BF-063 — "Recipes" (R) on any multi fluid identifier shows how to craft it.
 *
 * <p>JEI lists one identifier per fluid (a hundred-odd), and each is its own subtype, so R only
 * finds the crafting recipe for the blank identifier that recipe makes, which isn't in the list.
 * For an identifier set to a fluid this adds every crafting recipe whose result is the identifier
 * (so KubeJS changes to it show up too). Lookups only: the recipes themselves are JEI's vanilla
 * crafting rows, so {@link #getAllRecipes} is empty.
 */
final class FluidIdentifierRecipes
        implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {

    private static boolean isSetIdentifier(ITypedIngredient<?> ingredient) {
        ItemStack stack = ingredient.getItemStack().orElse(null);
        if (stack == null || !stack.is(ModItems.FLUID_IDENTIFIER.get())) return false;
        return !stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY)
                .equals(FluidIdentifierData.EMPTY);
    }

    private static List<RecipeHolder<CraftingRecipe>> craftingRecipes() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        return level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(
                        holder ->
                                holder.value()
                                        .getResultItem(level.registryAccess())
                                        .is(ModItems.FLUID_IDENTIFIER.get()))
                .toList();
    }

    @Override
    public boolean isHandledInput(ITypedIngredient<?> input) {
        return false;
    }

    @Override
    public boolean isHandledOutput(ITypedIngredient<?> output) {
        return isSetIdentifier(output);
    }

    @Override
    public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
        return List.of();
    }

    @Override
    public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
        return isSetIdentifier(output) ? craftingRecipes() : List.of();
    }

    @Override
    public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
        return List.of();
    }
}
