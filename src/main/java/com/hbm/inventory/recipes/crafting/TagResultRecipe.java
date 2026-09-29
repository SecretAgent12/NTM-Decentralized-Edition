// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import com.hbm.backport.recipe.NormalCraftingRecipe;
import com.hbm.backport.recipe.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import com.hbm.backport.RecipeSerializers;
import com.hbm.backport.Codecs;

public final class TagResultRecipe extends NormalCraftingRecipe {

    public static final MapCodec<TagResultRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            NormalCraftingRecipe.CommonInfo.MAP_CODEC.forGetter(
                                                    r -> r.commonInfo),
                                            NormalCraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(
                                                    r -> r.bookInfo),
                                            Result.CODEC.fieldOf("result").forGetter(r -> r.result),
                                            Ingredient.CODEC
                                                    .listOf(1, 9)
                                                    .fieldOf("ingredients")
                                                    .forGetter(r -> r.ingredients))
                                    .apply(i, TagResultRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TagResultRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    NormalCraftingRecipe.CommonInfo.STREAM_CODEC,
                    r -> r.commonInfo,
                    NormalCraftingRecipe.CraftingBookInfo.STREAM_CODEC,
                    r -> r.bookInfo,
                    Result.STREAM_CODEC,
                    r -> r.result,
                    Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    r -> r.ingredients,
                    TagResultRecipe::new);
    public static final RecipeSerializer<TagResultRecipe> SERIALIZER =
            RecipeSerializers.of(MAP_CODEC, STREAM_CODEC);

    private final Result result;
    private final List<Ingredient> ingredients;

    public TagResultRecipe(
            NormalCraftingRecipe.CommonInfo commonInfo,
            NormalCraftingRecipe.CraftingBookInfo bookInfo,
            Result result,
            List<Ingredient> ingredients) {
        super(commonInfo, bookInfo);
        this.result = result;
        this.ingredients = ingredients;
    }

    public Result result() {
        return result;
    }

    public Optional<Holder<Item>> resolve() {
        Iterator<Holder<Item>> members =
                BuiltInRegistries.ITEM.getTagOrEmpty(result.tag()).iterator();
        return members.hasNext() ? Optional.of(members.next()) : Optional.empty();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size() || resolve().isEmpty()) return false;

        return input.size() == 1 && ingredients.size() == 1
                ? ingredients.getFirst().test(input.getItem(0))
                : input.stackedContents().canCraft(this, null);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return resolve().map(item -> new ItemStack(item, result.count())).orElse(ItemStack.EMPTY);
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(ingredients);
    }

    // backport: 26.x recipe-book display() dropped; the 1.21.1 recipe book reads
    // getIngredients() and this.
    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return resolve().map(item -> new ItemStack(item, result.count())).orElse(ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<TagResultRecipe> getSerializer() {
        return SERIALIZER;
    }

    public record Result(TagKey<Item> tag, int count) {
        static final Codec<Result> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                TagKey.codec(Registries.ITEM)
                                                        .fieldOf("tag")
                                                        .forGetter(Result::tag),
                                                ExtraCodecs.POSITIVE_INT
                                                        .optionalFieldOf("count", 1)
                                                        .forGetter(Result::count))
                                        .apply(i, Result::new));
        static final StreamCodec<ByteBuf, Result> STREAM_CODEC =
                StreamCodec.composite(
                        Codecs.tagStreamCodec(Registries.ITEM),
                        Result::tag,
                        ByteBufCodecs.VAR_INT,
                        Result::count,
                        Result::new);
    }
}
