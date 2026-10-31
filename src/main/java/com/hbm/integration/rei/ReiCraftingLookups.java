// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.rei;

import com.hbm.integration.recipeviewer.CraftingRows;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * REI integration (our own, not in ntm-next or 1.7.10): crafting rows REI can't find by itself,
 * because the stack looked at is a subtype no recipe names.
 *
 * <ul>
 *   <li>fluid ducts stamped with a fluid: retyping and clearing ({@link CraftingRows});
 *   <li>a fluid identifier set to a fluid: every crafting recipe that makes the identifier (the
 *       recipe makes a blank one, and each fluid is its own entry), as BF-063 does for JEI.
 * </ul>
 */
final class ReiCraftingLookups implements DynamicDisplayGenerator<DefaultCraftingDisplay<?>> {

    private final CraftingRows rows = new CraftingRows();

    @Override
    public Optional<List<DefaultCraftingDisplay<?>>> getRecipeFor(EntryStack<?> entry) {
        if (!(entry.getValue() instanceof ItemStack stack)) return Optional.empty();
        List<RecipeHolder<CraftingRecipe>> found = new ArrayList<>(rows.recipesFor(stack));
        if (isSetIdentifier(stack)) found.addAll(identifierRecipes());
        return displays(found);
    }

    @Override
    public Optional<List<DefaultCraftingDisplay<?>>> getUsageFor(EntryStack<?> entry) {
        if (!(entry.getValue() instanceof ItemStack stack)) return Optional.empty();
        return displays(rows.usesOf(stack));
    }

    private static Optional<List<DefaultCraftingDisplay<?>>> displays(
            List<RecipeHolder<CraftingRecipe>> recipes) {
        if (recipes.isEmpty()) return Optional.empty();
        List<DefaultCraftingDisplay<?>> out = new ArrayList<>(recipes.size());
        for (RecipeHolder<CraftingRecipe> recipe : recipes) {
            DefaultCraftingDisplay<?> display = DefaultCraftingDisplay.of(recipe);
            if (display != null) out.add(display);
        }
        return out.isEmpty() ? Optional.empty() : Optional.of(out);
    }

    private static boolean isSetIdentifier(ItemStack stack) {
        if (!stack.is(ModItems.FLUID_IDENTIFIER.get())) return false;
        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        return !FluidIdentifierData.EMPTY.equals(data);
    }

    private static List<RecipeHolder<CraftingRecipe>> identifierRecipes() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        List<RecipeHolder<CraftingRecipe>> out = new ArrayList<>(0x8);
        for (RecipeHolder<CraftingRecipe> holder :
                level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (holder.value().getResultItem(level.registryAccess()).is(ModItems.FLUID_IDENTIFIER.get()))
                out.add(holder);
        }
        return out;
    }
}
