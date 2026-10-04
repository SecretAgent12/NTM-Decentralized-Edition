// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.kubejs;

import com.hbm.backport.ItemStackTemplate;
import com.hbm.backport.random.WeightedList;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.component.SizedIngredientComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.util.IntBounds;
import java.util.List;
import java.util.Optional;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * backport: KubeJS integration (pack makers asked for it). NTM machine recipes are plain datapack
 * recipes, so KubeJS can already remove them and add raw JSON ones; this plugin teaches it NTM's
 * field formats so scripts get typed builders ({@code event.recipes.hbm.assembly_machine(...)}),
 * {@code replaceInput}/{@code replaceOutput} and item/fluid filters on machine recipes.
 *
 * <p>Loaded only by KubeJS (listed in kubejs.plugins.txt); nothing here runs without it.
 */
public class NTMKubeJSPlugin implements KubeJSPlugin {

    /** NTM recipe input ({"ingredient", "count", "components"}) as a KubeJS sized ingredient. */
    private static final Codec<SizedIngredient> COUNT_INGREDIENT_CODEC =
            CountIngredient.CODEC.xmap(
                    in -> new SizedIngredient(in.ingredient(), in.count()),
                    in -> new CountIngredient(in.ingredient(), in.count()));

    public static final RecipeComponentType<SizedIngredient> COUNT_INGREDIENT =
            RecipeComponentType.unit(
                    Library.id("count_ingredient"),
                    type -> new SizedIngredientComponent(type, COUNT_INGREDIENT_CODEC, false));

    public static final RecipeComponentType<WeightedList<Optional<ItemStackTemplate>>> OUTPUT =
            RecipeComponentType.unit(Library.id("output"), NtmOutputComponent::new);

    public static final RecipeComponentType<FluidStackNTM> FLUID =
            RecipeComponentType.unit(Library.id("fluid"), NtmFluidComponent::new);

    /**
     * Recipe types read by GenericRecipe's common fields. Type-specific extras (laser strength,
     * stamp, tier...) stay untouched on existing recipes; new ones set them with .merge({...}).
     */
    private static final List<String> MACHINE_TYPES =
            List.of(
                    "anvil_construction", "anvil_smithing", "arc_furnace", "arc_welder",
                    "assembly_machine", "blast_furnace", "breeder", "catalytic_reformer",
                    "centrifuge", "chemical_plant", "coker", "combination_oven", "compressor",
                    "cracking_tower", "crystallizer", "custom_machine", "cyclotron",
                    "electrolyser_fluid", "electrolyser_metal", "exposure_chamber",
                    "fluid_breeder", "fraction_tower", "fuel_pool", "fusion", "hydrotreater",
                    "lemegeton", "liquefaction", "magic", "mixer", "outgasser",
                    "particle_accelerator", "plasma_forge", "precision_assembler", "press",
                    "purex", "pyro_oven", "radiolysis", "refinery", "rock_mill", "shredder",
                    "silex", "soldering", "solidification", "space_assembler", "supercomputer",
                    "vacuum_refinery");

    private static final IntBounds ANY_SIZE = new IntBounds(0, Integer.MAX_VALUE);

    @Override
    public void registerRecipeComponents(RecipeComponentTypeRegistry registry) {
        registry.register(COUNT_INGREDIENT);
        registry.register(OUTPUT);
        registry.register(FLUID);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        for (String type : MACHINE_TYPES) registry.register(Library.id(type), machineSchema());
    }

    private static RecipeSchema machineSchema() {
        RecipeKey<List<SizedIngredient>> inputItems =
                list(COUNT_INGREDIENT.instance()).inputKey("input_items").optional(List.of());
        RecipeKey<List<FluidStackNTM>> inputFluids =
                list(FLUID.instance()).inputKey("input_fluids").optional(List.of());
        RecipeKey<List<WeightedList<Optional<ItemStackTemplate>>>> outputItems =
                list(OUTPUT.instance()).outputKey("output_items").optional(List.of());
        RecipeKey<List<FluidStackNTM>> outputFluids =
                list(FLUID.instance()).outputKey("output_fluids").optional(List.of());
        RecipeKey<Integer> duration =
                NumberComponent.NON_NEGATIVE_INT.instance().otherKey("duration").optional(0);
        RecipeKey<Long> power =
                NumberComponent.NON_NEGATIVE_LONG.instance().otherKey("power").optional(0L);

        return new RecipeSchema(
                        outputItems, inputItems, outputFluids, inputFluids, duration, power)
                .constructor(outputItems, inputItems)
                .constructor(outputItems, inputItems, duration)
                .constructor(outputItems, inputItems, duration, power);
    }

    private static <T> ListRecipeComponent<T> list(RecipeComponent<T> component) {
        return ListRecipeComponent.create(component, false, false, ANY_SIZE, Optional.empty());
    }
}
