// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.kubejs;

import com.hbm.backport.ItemStackTemplate;
import com.hbm.backport.random.WeightedList;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.component.SizedIngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
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

    public static final RecipeComponentType<MaterialStack> MATERIAL =
            RecipeComponentType.unit(Library.id("material"), NtmMaterialComponent::new);

    /**
     * Recipe types read by GenericRecipe's common fields (rotary furnace, crucible, gas centrifuge,
     * ammo press and pedestal have their own schemas below). Type-specific extras (laser strength,
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
        registry.register(MATERIAL);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        for (String type : MACHINE_TYPES) registry.register(Library.id(type), machineSchema());
        registry.register(Library.id("rotary_furnace"), rotaryFurnaceSchema());
        registry.register(Library.id("crucible"), crucibleSchema());
        registry.register(Library.id("gas_centrifuge"), gasCentrifugeSchema());
        registry.register(Library.id("ammo_press"), gridSchema(false));
        registry.register(Library.id("pedestal"), gridSchema(true));
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

    private static RecipeKey<Integer> duration() {
        return NumberComponent.NON_NEGATIVE_INT.instance().otherKey("duration").optional(0);
    }

    private static RecipeKey<Long> power() {
        return NumberComponent.NON_NEGATIVE_LONG.instance().otherKey("power").optional(0L);
    }

    /** Rotary furnace: items and fluid in, one foundry material out, steam per operation. */
    private static RecipeSchema rotaryFurnaceSchema() {
        var output = MATERIAL.instance().outputKey("output_material");
        var inputItems =
                list(COUNT_INGREDIENT.instance()).inputKey("input_items").optional(List.of());
        var inputFluids = list(FLUID.instance()).inputKey("input_fluids").optional(List.of());
        var duration = duration();
        var power = power();
        var steam = NumberComponent.NON_NEGATIVE_INT.instance().otherKey("steam").optional(0);
        return new RecipeSchema(output, inputItems, inputFluids, duration, power, steam)
                .constructor(output, inputItems)
                .constructor(output, inputItems, duration);
    }

    /** Crucible alloying: foundry materials in, foundry materials out (slag included). */
    private static RecipeSchema crucibleSchema() {
        var output = list(MATERIAL.instance()).outputKey("output_materials");
        var input = list(MATERIAL.instance()).inputKey("input_materials");
        var frequency =
                NumberComponent.POSITIVE_INT.instance().otherKey("frequency").optional(1);
        return new RecipeSchema(output, input, frequency)
                .constructor(output, input)
                .constructor(output, input, frequency);
    }

    /** Gas centrifuge stage: the stage's fluid in (feed/consumed), items and the next stage out. */
    private static RecipeSchema gasCentrifugeSchema() {
        var outputItems =
                list(OUTPUT.instance()).outputKey("output_items").optional(List.of());
        var stage = StringComponent.STRING.instance().otherKey("stage");
        var consumed =
                NumberComponent.NON_NEGATIVE_INT.instance().otherKey("consumed").optional(0);
        var produced =
                NumberComponent.NON_NEGATIVE_INT.instance().otherKey("produced").optional(0);
        var next = StringComponent.STRING.instance().otherKey("next").defaultOptional();
        var requiresUpgrade =
                BooleanComponent.BOOLEAN.instance().otherKey("requires_upgrade").optional(false);
        var feed = StringComponent.ID.instance().inputKey("feed").defaultOptional();
        var deadEndVolume =
                NumberComponent.NON_NEGATIVE_INT
                        .instance()
                        .otherKey("dead_end_volume")
                        .optional(0);
        var deadEndItems =
                list(ItemStackComponent.ITEM_STACK.instance())
                        .outputKey("dead_end_items")
                        .optional(List.of());
        var duration = duration();
        var power = power();
        return new RecipeSchema(
                        outputItems, stage, consumed, produced, next, requiresUpgrade, feed,
                        deadEndVolume, deadEndItems, duration, power)
                .constructor(outputItems, stage)
                .constructor(outputItems, stage, duration);
    }

    /**
     * Ammo press and pedestal: a 3x3 grid like a shaped crafting recipe (pattern + key, each key an
     * NTM count ingredient). The pedestal adds its {@code extra} condition and {@code recipe_set}.
     */
    private static RecipeSchema gridSchema(boolean pedestal) {
        var outputItems = list(OUTPUT.instance()).outputKey("output_items");
        var pattern = list(StringComponent.STRING.instance()).otherKey("pattern");
        var key = COUNT_INGREDIENT.instance().asPatternKey().inputKey("key");
        if (!pedestal) {
            var duration = duration();
            var power = power();
            return new RecipeSchema(outputItems, pattern, key, duration, power)
                    .constructor(outputItems, pattern, key);
        }
        var extra = StringComponent.STRING.instance().otherKey("extra").defaultOptional();
        var recipeSet =
                NumberComponent.NON_NEGATIVE_INT.instance().otherKey("recipe_set").optional(0);
        return new RecipeSchema(outputItems, pattern, key, extra, recipeSet)
                .constructor(outputItems, pattern, key);
    }

    private static <T> ListRecipeComponent<T> list(RecipeComponent<T> component) {
        return ListRecipeComponent.create(component, false, false, ANY_SIZE, Optional.empty());
    }
}
