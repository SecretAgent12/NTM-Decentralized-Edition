// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.recipe;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 26.x PlacementInfo: how the recipe book places a recipe's ingredients in a grid.
 * 1.21.1 derives that from Recipe.getIngredients(), so this only keeps the
 * ingredient list for code that asks for it.
 */
public final class PlacementInfo {

    public static final PlacementInfo NOT_PLACEABLE = new PlacementInfo(List.of(), true);

    private final List<Ingredient> ingredients;
    private final boolean impossible;

    private PlacementInfo(List<Ingredient> ingredients, boolean impossible) {
        this.ingredients = ingredients;
        this.impossible = impossible;
    }

    public static PlacementInfo create(Ingredient ingredient) {
        return new PlacementInfo(List.of(ingredient), ingredient.isEmpty());
    }

    public static PlacementInfo create(List<Ingredient> ingredients) {
        return new PlacementInfo(List.copyOf(ingredients), ingredients.stream().anyMatch(Ingredient::isEmpty));
    }

    public static PlacementInfo createFromOptionals(List<Optional<Ingredient>> ingredients) {
        return create(ingredients.stream().map(o -> o.orElse(Ingredient.EMPTY)).toList());
    }

    public List<Ingredient> ingredients() {
        return ingredients;
    }

    public boolean isImpossibleToPlace() {
        return impossible;
    }
}
