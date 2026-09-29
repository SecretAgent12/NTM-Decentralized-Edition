// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import com.hbm.backport.RecipeSerializers;

public class ContainerUpgradeRecipe extends NormalCraftingRecipe {

    public static final MapCodec<ContainerUpgradeRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            NormalCraftingRecipe.CommonInfo.MAP_CODEC.forGetter(
                                                    o -> o.commonInfo),
                                            NormalCraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(
                                                    o -> o.bookInfo),
                                            ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                                            ItemStackTemplate.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(o -> o.result),
                                            Ingredient.CODEC
                                                    .fieldOf("carrier")
                                                    .forGetter(o -> o.carrier))
                                    .apply(i, ContainerUpgradeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerUpgradeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    NormalCraftingRecipe.CommonInfo.STREAM_CODEC,
                    o -> o.commonInfo,
                    NormalCraftingRecipe.CraftingBookInfo.STREAM_CODEC,
                    o -> o.bookInfo,
                    ShapedRecipePattern.STREAM_CODEC,
                    o -> o.pattern,
                    ItemStackTemplate.STREAM_CODEC,
                    o -> o.result,
                    Ingredient.CONTENTS_STREAM_CODEC,
                    o -> o.carrier,
                    ContainerUpgradeRecipe::new);
    public static final RecipeSerializer<ContainerUpgradeRecipe> SERIALIZER =
            RecipeSerializers.of(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;
    private final Ingredient carrier;

    public ContainerUpgradeRecipe(
            NormalCraftingRecipe.CommonInfo commonInfo,
            NormalCraftingRecipe.CraftingBookInfo bookInfo,
            ShapedRecipePattern pattern,
            ItemStackTemplate result,
            Ingredient carrier) {
        super(commonInfo, bookInfo);
        this.pattern = pattern;
        this.result = result;
        this.carrier = carrier;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && carrier.test(stack)) {
                return result.apply(result.count(), stack.getComponentsPatch());
            }
        }
        assert false : "carrier ingredient absent from a matched grid";
        return result.create();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(pattern.ingredients());
    }

    // backport: 26.x recipe-book display() dropped; the 1.21.1 recipe book reads
    // getIngredients(), canCraftInDimensions() and getResultItem().
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.width() && height >= pattern.height();
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return result.create();
    }

    @Override
    public RecipeSerializer<ContainerUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
