// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import com.hbm.items.machine.ItemStamp.StampType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class PressRecipes extends SimpleGenericRecipes<PressRecipe> {

    public static final PressRecipes INSTANCE = new PressRecipes();

    @Override
    protected String registryName() {
        return "press";
    }

    @Override
    protected RecipeSerializer<PressRecipe> serializer() {
        return PressRecipe.SERIALIZER;
    }

    public ItemStack getOutput(ItemStack input, @Nullable StampType stampType) {
        PressRecipe recipe = getRecipe(input, stampType);
        return recipe == null ? ItemStack.EMPTY : recipe.output().copy();
    }

    public @Nullable PressRecipe getRecipe(ItemStack input, @Nullable StampType stampType) {
        if (input.isEmpty() || stampType == null) return null;
        String name = StampTypes.name(stampType);
        return findByItem(
                input, r -> name.equals(r.stampType) && r.inputItem[0].matchesItem(input));
    }

    // backport-fix: BF-128 (GitHub #47) by the item in the stamp slot, with every type it has
    public ItemStack getOutput(ItemStack input, ItemStack stamp) {
        PressRecipe recipe = getRecipe(input, stamp);
        return recipe == null ? ItemStack.EMPTY : recipe.output().copy();
    }

    public @Nullable PressRecipe getRecipe(ItemStack input, ItemStack stamp) {
        if (input.isEmpty()) return null;
        Set<String> types = StampTypes.typesOf(stamp);
        if (types.isEmpty()) return null;
        return findByItem(
                input, r -> types.contains(r.stampType) && r.inputItem[0].matchesItem(input));
    }
}
