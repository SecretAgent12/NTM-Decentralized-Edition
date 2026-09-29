// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.NuclearTech;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.material.MaterialDistributionRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.Registries;

public final class DatapackRecipeLoad {

    private DatapackRecipeLoad() {}

    public static void apply(
            Collection<RecipeHolder<?>> holders, HolderLookup.Provider registries) {
        for (RecipeHolder<?> holder : holders) {
            if (holder.value() instanceof GenericRecipe row) row.bind(ResourceKey.create(Registries.RECIPE, holder.id()));
        }
        validateUniqueKeys(holders);
        Map<ResourceKey<CustomMachineDefinition>, CustomMachineDefinition> definitions =
                new LinkedHashMap<>();
        registries
                .lookup(CustomMachineDefinition.REGISTRY)
                .ifPresent(
                        lookup ->
                                lookup.listElements()
                                        .forEach(
                                                entry ->
                                                        definitions.put(
                                                                entry.key(), entry.value())));
        for (String warning : warnings(holders, definitions)) NuclearTech.LOGGER.warn(warning);
    }

    private static void validateUniqueKeys(Collection<RecipeHolder<?>> holders) {
        Map<Object, ResourceLocation> materials = new HashMap<>();
        Map<String, ResourceLocation> stages = new HashMap<>();
        Map<Fluid, ResourceLocation> feeds = new HashMap<>();
        for (RecipeHolder<?> holder : holders) {
            ResourceLocation id = holder.id();
            if (holder.value() instanceof MaterialDistributionRecipe row) {
                row.targets()
                        .unwrap()
                        .ifLeft(tag -> claim(materials, tag, id, "material_distribution target"))
                        .ifRight(
                                items ->
                                        items.forEach(
                                                item ->
                                                        claim(
                                                                materials,
                                                                item.value(),
                                                                id,
                                                                "material_distribution target")));
            } else if (holder.value() instanceof GasCentrifugeRecipe row) {
                claim(stages, row.stage, id, "gas_centrifuge stage");
                if (row.feed != null)
                    claim(
                            feeds,
                            row.feed,
                            id,
                            "gas_centrifuge feed " + BuiltInRegistries.FLUID.getKey(row.feed));
            }
        }
    }

    private static <K> void claim(Map<K, ResourceLocation> claims, K key, ResourceLocation id, String kind) {
        ResourceLocation prior = claims.putIfAbsent(key, id);
        if (prior != null) {
            throw new IllegalStateException(
                    "Duplicate " + kind + " '" + key + "' in " + prior + " and " + id);
        }
    }

    public static List<String> warnings(
            Collection<RecipeHolder<?>> holders,
            Map<ResourceKey<CustomMachineDefinition>, CustomMachineDefinition> definitions) {
        List<String> out = new ArrayList<>();
        duplicateNames(holders, out);
        customMachines(holders, definitions, out);
        return out;
    }

    private static void duplicateNames(Collection<RecipeHolder<?>> holders, List<String> out) {
        Map<RecipeType<?>, Map<String, List<ResourceKey<Recipe<?>>>>> byTable =
                new LinkedHashMap<>();
        for (RecipeHolder<?> holder : holders) {
            if (!(holder.value() instanceof GenericRecipe row)) continue;
            byTable.computeIfAbsent(row.getType(), type -> new LinkedHashMap<>())
                    .computeIfAbsent(row.getInternalName(), name -> new ArrayList<>())
                    .add(ResourceKey.create(Registries.RECIPE, holder.id()));
        }
        byTable.forEach(
                (table, names) ->
                        names.forEach(
                                (name, ids) -> {
                                    if (ids.size() > 1) {
                                        out.add(
                                                table
                                                        + ": recipe name '"
                                                        + name
                                                        + "' is shared by "
                                                        + ids.stream()
                                                                .map(
                                                                        id ->
                                                                                id.location()
                                                                                        .toString())
                                                                .toList()
                                                        + "; a lookup by name reaches only one");
                                    }
                                }));
    }

    private static void customMachines(
            Collection<RecipeHolder<?>> holders,
            Map<ResourceKey<CustomMachineDefinition>, CustomMachineDefinition> definitions,
            List<String> out) {
        Map<String, List<ResourceKey<CustomMachineDefinition>>> byKey = new HashMap<>();
        definitions.forEach(
                (key, definition) ->
                        byKey.computeIfAbsent(definition.recipeKey(), k -> new ArrayList<>())
                                .add(key));

        Set<String> keysWithRows = new HashSet<>();
        for (RecipeHolder<?> holder : holders) {
            if (!(holder.value() instanceof CustomMachineRecipe row)) continue;
            keysWithRows.add(row.recipeKey);
            List<ResourceKey<CustomMachineDefinition>> named = byKey.get(row.recipeKey);
            String id = holder.id().toString();
            if (named == null) {
                out.add(
                        "custom machine recipe "
                                + id
                                + ": no hbm:custom_machine definition names recipe_key '"
                                + row.recipeKey
                                + "'");
                continue;
            }
            Slots wanted = Slots.of(row);
            if (named.stream().noneMatch(key -> wanted.fitsIn(Slots.of(definitions.get(key))))) {
                List<String> offered =
                        named.stream()
                                .map(key -> key.location() + " " + Slots.of(definitions.get(key)))
                                .toList();
                out.add(
                        "custom machine recipe "
                                + id
                                + " wants "
                                + wanted
                                + " but every definition naming "
                                + "recipe_key '"
                                + row.recipeKey
                                + "' holds fewer: "
                                + offered);
            }
        }
        definitions.forEach(
                (key, definition) -> {
                    if (!keysWithRows.contains(definition.recipeKey())) {
                        out.add(
                                "custom machine "
                                        + key.location()
                                        + ": no hbm:custom_machine recipe carries recipe_key '"
                                        + definition.recipeKey()
                                        + "'");
                    }
                });
    }

    private record Slots(int itemIn, int itemOut, int fluidIn, int fluidOut) {

        static Slots of(CustomMachineRecipe row) {
            return new Slots(
                    row.inputItem.length,
                    row.outputTemplates().size(),
                    row.inputFluid.length,
                    row.outputFluid.length);
        }

        static Slots of(CustomMachineDefinition definition) {
            return new Slots(
                    definition.itemInCount(),
                    definition.itemOutCount(),
                    definition.fluidInCount(),
                    definition.fluidOutCount());
        }

        boolean fitsIn(Slots held) {
            return itemIn <= held.itemIn
                    && itemOut <= held.itemOut
                    && fluidIn <= held.fluidIn
                    && fluidOut <= held.fluidOut;
        }

        @Override
        public String toString() {
            return "(item in "
                    + itemIn
                    + ", item out "
                    + itemOut
                    + ", fluid in "
                    + fluidIn
                    + ", fluid out "
                    + fluidOut
                    + ")";
        }
    }
}
