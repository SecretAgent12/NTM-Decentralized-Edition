// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.hbm.backport.recipe.DataComponentMatchers;
import com.hbm.backport.recipe.Ingredients;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

// backport: 26.x StackedContents.IngredientInfo does not exist in 1.21.1 (StackedContents is
// not generic there); acceptsItem stays as a plain method.
public record CountIngredient(Ingredient ingredient, int count) {

    public static final MapCodec<CountIngredient> MAP_CODEC =
            DataCodecs.strict(
                    RecordCodecBuilder.mapCodec(
                            i ->
                                    i.group(
                                                    Ingredient.CODEC
                                                            .fieldOf("ingredient")
                                                            .forGetter(
                                                                    CountIngredient
                                                                            ::baseIngredient),
                                                    ExtraCodecs.POSITIVE_INT
                                                            .optionalFieldOf("count", 1)
                                                            .forGetter(CountIngredient::count),
                                                    DataComponentMatchers.CODEC.forGetter(
                                                            CountIngredient::components))
                                            .apply(i, CountIngredient::withMatchers)));

    public static final Codec<CountIngredient> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, CountIngredient> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    CountIngredient::ingredient,
                    ByteBufCodecs.VAR_INT,
                    CountIngredient::count,
                    CountIngredient::new);

    public static CountIngredient of(ItemLike item) {
        return new CountIngredient(Ingredient.of(item), 1);
    }

    public static CountIngredient of(ItemLike item, int count) {
        return new CountIngredient(Ingredient.of(item), count);
    }

    public static CountIngredient of(HolderSet<Item> tag, int count) {
        return new CountIngredient(Ingredients.of(tag), count);
    }

    public static CountIngredient of(TagKey<Item> tag, int count) {
        return new CountIngredient(Ingredient.of(tag), count);
    }

    public static <T> CountIngredient of(
            ItemLike item, int count, DataComponentType<T> type, T value) {
        return withMatchers(
                Ingredient.of(item),
                count,
                new DataComponentMatchers(
                        DataComponentPredicate.builder().expect(type, value).build(), Map.of()));
    }

    public static CountIngredient ofContent(ItemLike item, int count, Fluid fluid) {
        return withMatchers(
                Ingredient.of(item),
                count,
                new DataComponentMatchers(
                        DataComponentPredicate.EMPTY,
                        Map.<ItemSubPredicate.Type<?>, ItemSubPredicate>of(
                                FluidContentPredicate.TYPE.get(),
                                new FluidContentPredicate(fluid))));
    }

    public static CountIngredient ofFluidContent(Fluid fluid, int amount, int count) {
        return new CountIngredient(new HbmFluidContentIngredient(fluid, amount).toVanilla(), count);
    }

    public static CountIngredient withMatchers(
            Ingredient base, int count, DataComponentMatchers matchers) {
        if (matchers.isEmpty()) return new CountIngredient(base, count);
        return new CountIngredient(new HbmMatchersIngredient(base, matchers).toVanilla(), count);
    }

    public DataComponentMatchers components() {

        var custom = ingredient.getCustomIngredient();
        return custom instanceof HbmMatchersIngredient matchers
                ? matchers.matchers()
                : DataComponentMatchers.ANY;
    }

    private Ingredient baseIngredient() {
        var custom = ingredient.getCustomIngredient();
        return custom instanceof HbmMatchersIngredient matchers ? matchers.base() : ingredient;
    }

    public boolean test(ItemStack stack) {
        return matchesItem(stack) && stack.getCount() >= count;
    }

    public boolean matchesItem(ItemStack stack) {

        if (stack.isDamaged()) {
            Ingredient base = baseIngredient();
            if (base.getCustomIngredient() == null && !Ingredients.isTag(base)) return false;
        }
        return ingredient.test(stack);
    }

    public boolean acceptsItem(ItemStack stack) {
        return matchesItem(stack);
    }

    public Optional<Fluid> contentFluid() {
        ItemSubPredicate content = components().partial().get(FluidContentPredicate.TYPE.get());
        return content instanceof FluidContentPredicate(Fluid fluid1)
                ? Optional.of(fluid1)
                : Optional.empty();
    }

    public ItemStack extractForCyclingDisplay(int cycle) {
        return extractForCyclingDisplay(System.currentTimeMillis(), cycle);
    }

    public ItemStack extractForCyclingDisplay(long millis, int cycle) {
        List<ItemStack> stacks = displayStacks();

        if (stacks.isEmpty()) return ItemStack.EMPTY;
        long span = cycle * 50L;
        return stacks.get((int) (millis % (span * stacks.size()) / span));
    }

    public List<ItemStack> displayStacks() {
        return displayStacks(ingredient, count);
    }

    public static List<ItemStack> displayStacks(Ingredient ingredient, int count) {
        List<ItemStack> stacks = new ArrayList<>(Ingredients.displayStacks(ingredient));
        stacks.replaceAll(stack -> stack.copyWithCount(count));
        return stacks;
    }
}
