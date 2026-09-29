// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.recipe;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 26.x Ingredient accessors over the 1.21.1 Ingredient. 26.x ingredients expose
 * their items as holders (items()) and a SlotDisplay for the recipe book
 * (display()); 1.21.1 ingredients expose getItems(), the stacks the recipe book
 * and JEI cycle through.
 */
public final class Ingredients {

    private Ingredients() {}

    /** 26.x Ingredient.items(). */
    public static Stream<Holder<Item>> items(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems()).map(ItemStack::getItemHolder).distinct();
    }

    /** What 26.x got from display().resolveForStacks(...): the stacks shown for it. */
    public static List<ItemStack> displayStacks(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems()).map(ItemStack::copy).toList();
    }

    /** 26.x `display() instanceof TagSlotDisplay`: the ingredient is a plain item tag. */
    public static boolean isTag(Ingredient ingredient) {
        return !ingredient.isCustom()
                && Arrays.stream(ingredient.getValues()).anyMatch(v -> v instanceof Ingredient.TagValue);
    }

    /** 26.x Ingredient.of(HolderSet): a named set becomes a tag ingredient. */
    public static Ingredient of(HolderSet<Item> items) {
        return items.unwrapKey()
                .map(Ingredient::of)
                .orElseGet(() -> Ingredient.of(items.stream().map(h -> new ItemStack(h))));
    }
}
