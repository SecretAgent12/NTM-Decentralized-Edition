// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 26.x NormalCraftingRecipe (a crafting recipe with the shared "show_notification",
 * "category" and "group" fields) on 1.21.1's CraftingRecipe. 26.x keeps those fields in
 * Recipe.CommonInfo and CraftingRecipe.CraftingBookInfo; 1.21.1 has no such types, so
 * they are nested here with the same JSON shape.
 *
 * 1.21.1 describes grid placement with getIngredients() / canCraftInDimensions() where
 * 26.x has PlacementInfo; both are derived from createPlacementInfo().
 */
public abstract class NormalCraftingRecipe implements CraftingRecipe {

    public record CommonInfo(boolean showNotification) {
        public static final MapCodec<CommonInfo> MAP_CODEC =
                Codec.BOOL.optionalFieldOf("show_notification", true).xmap(CommonInfo::new, CommonInfo::showNotification);
        public static final StreamCodec<ByteBuf, CommonInfo> STREAM_CODEC =
                ByteBufCodecs.BOOL.map(CommonInfo::new, CommonInfo::showNotification);
    }

    public record CraftingBookInfo(CraftingBookCategory category, String group) {
        public static final MapCodec<CraftingBookInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC)
                        .forGetter(CraftingBookInfo::category),
                Codec.STRING.optionalFieldOf("group", "").forGetter(CraftingBookInfo::group))
                .apply(i, CraftingBookInfo::new));
        public static final StreamCodec<ByteBuf, CraftingBookInfo> STREAM_CODEC = StreamCodec.composite(
                CraftingBookCategory.STREAM_CODEC, CraftingBookInfo::category,
                ByteBufCodecs.STRING_UTF8, CraftingBookInfo::group,
                CraftingBookInfo::new);
    }

    protected final CommonInfo commonInfo;
    protected final CraftingBookInfo bookInfo;
    private NonNullList<Ingredient> ingredients;

    protected NormalCraftingRecipe(CommonInfo commonInfo, CraftingBookInfo bookInfo) {
        this.commonInfo = commonInfo;
        this.bookInfo = bookInfo;
    }

    public abstract ItemStack assemble(CraftingInput input);

    protected abstract PlacementInfo createPlacementInfo();

    @Override
    public final ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return assemble(input);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        if (ingredients == null) {
            ingredients = NonNullList.create();
            ingredients.addAll(createPlacementInfo().ingredients());
        }
        return ingredients;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= getIngredients().size();
    }

    /** 26.x recipes have no fixed result; subclasses that have one return it for the recipe book. */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return commonInfo.showNotification();
    }

    @Override
    public String getGroup() {
        return bookInfo.group();
    }

    @Override
    public CraftingBookCategory category() {
        return bookInfo.category();
    }
}
