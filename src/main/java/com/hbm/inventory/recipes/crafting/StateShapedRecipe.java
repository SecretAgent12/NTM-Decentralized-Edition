// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.RecipeSerializers;
import com.hbm.backport.recipe.NormalCraftingRecipe;
import com.hbm.backport.recipe.TransmuteRecipe;

public final class StateShapedRecipe extends ShapedRecipe implements CraftingInputState {
    private static final MapCodec<Definition> DEFINITION_CODEC =
            DataCodecs.recipe(
                    RecordCodecBuilder.mapCodec(
                            i ->
                                    i.group(
                                                    NormalCraftingRecipe.CommonInfo.MAP_CODEC
                                                            .forGetter(Definition::commonInfo),
                                                    NormalCraftingRecipe.CraftingBookInfo.MAP_CODEC
                                                            .forGetter(Definition::bookInfo),
                                                    ShapedRecipePattern.Data.MAP_CODEC.forGetter(
                                                            Definition::pattern),
                                                    ItemStackTemplate.CODEC
                                                            .fieldOf("result")
                                                            .forGetter(Definition::result),
                                                    Codec.unboundedMap(
                                                                    Codec.STRING,
                                                                    InputStateRule.CODEC)
                                                            .optionalFieldOf(
                                                                    "input_state", Map.of())
                                                            .forGetter(Definition::inputState))
                                            .apply(i, Definition::new)));
    public static final MapCodec<StateShapedRecipe> MAP_CODEC =
            DEFINITION_CODEC.flatXmap(
                    StateShapedRecipe::decode,
                    recipe ->
                            DataResult.success(
                                    new Definition(
                                            recipe.commonInfo,
                                            recipe.bookInfo,
                                            recipe.data,
                                            recipe.result,
                                            recipe.inputState)));
    public static final StreamCodec<RegistryFriendlyByteBuf, StateShapedRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());
    public static final RecipeSerializer<StateShapedRecipe> SERIALIZER =
            RecipeSerializers.of(MAP_CODEC, STREAM_CODEC);
    // backport: 1.21.1 ShapedRecipe keeps group/category/showNotification as plain fields, not
    // 26.x's CommonInfo/CraftingBookInfo, so the codec's copies live here
    private final NormalCraftingRecipe.CommonInfo commonInfo;
    private final NormalCraftingRecipe.CraftingBookInfo bookInfo;
    private final ShapedRecipePattern.Data data;
    private final ItemStackTemplate result;
    private final Map<String, InputStateRule> inputState;
    private final InputStateRule.Bound[] cells;
    private final int carrier;
    private final int ingredientCount;

    // backport: 26.x (Java 25) validates before super(...); Java 21 needs super first, so the
    // validation runs in compile() and this constructor only passes its result on
    private StateShapedRecipe(Definition definition) {
        this(definition, compile(definition));
    }

    private StateShapedRecipe(Definition definition, Compiled compiled) {
        super(
                definition.bookInfo.group(),
                definition.bookInfo.category(),
                new ShapedRecipePattern(
                        compiled.width,
                        compiled.height,
                        placement(compiled.cells),
                        Optional.empty()),
                definition.result.create(),
                definition.commonInfo.showNotification());
        commonInfo = definition.commonInfo;
        bookInfo = definition.bookInfo;
        this.data = definition.pattern;
        this.result = definition.result;
        this.inputState = compiled.inputState;
        this.cells = compiled.cells;
        carrier = compiled.carrier;
        ingredientCount = (int) Arrays.stream(cells).filter(Objects::nonNull).count();
    }

    private static NonNullList<Ingredient> placement(InputStateRule.Bound[] cells) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(cells.length, Ingredient.EMPTY);
        for (int index = 0; index < cells.length; index++)
            if (cells[index] != null) ingredients.set(index, cells[index].placementIngredient());
        return ingredients;
    }

    private record Compiled(
            Map<String, InputStateRule> inputState,
            InputStateRule.Bound[] cells,
            int width,
            int height,
            int carrier) {}

    private static Compiled compile(Definition definition) {
        ShapedRecipePattern.Data data = definition.pattern;
        ItemStackTemplate result = definition.result;
        Map<String, InputStateRule> inputState = Map.copyOf(definition.inputState);
        int left = 3;
        int top = 3;
        for (int y = 0; y < data.pattern().size(); y++) {
            String row = data.pattern().get(y);
            for (int x = 0; x < row.length(); x++)
                if (row.charAt(x) != ' ') {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                }
        }
        if (top == 3) throw new IllegalArgumentException("pattern contains no ingredients");
        ShapedRecipePattern pattern = ShapedRecipePattern.of(data.key(), data.pattern());
        for (String symbol : inputState.keySet()) {
            if (symbol.length() != 1 || !data.key().containsKey(symbol.charAt(0)))
                throw new IllegalArgumentException(
                        "input_state references undefined pattern symbol '" + symbol + "'");
        }
        InputStateRule.Bound[] cells = new InputStateRule.Bound[pattern.width() * pattern.height()];
        int resultCarrier = -1;
        for (int y = 0; y < pattern.height(); y++)
            for (int x = 0; x < pattern.width(); x++) {
                char symbol = data.pattern().get(y + top).charAt(x + left);
                if (symbol == ' ') continue;
                int index = x + y * pattern.width();
                cells[index] =
                        inputState
                                .getOrDefault(String.valueOf(symbol), InputStateRule.STRICT)
                                .bind(data.key().get(symbol));
                if (cells[index].preserve() == InputStateRule.Preservation.RESULT) {
                    if (resultCarrier != -1)
                        throw new IllegalArgumentException(
                                "input_state result preservation requires exactly one carrier occurrence");
                    resultCarrier = index;
                }
            }
        if (resultCarrier != -1) {
            InputStateRule.Bound bound = cells[resultCarrier];
            String error =
                    new InputStateRule(bound.matchers(), bound.preserve()).resultError(result);
            if (error != null) throw new IllegalArgumentException(error);
        }
        return new Compiled(inputState, cells, pattern.width(), pattern.height(), resultCarrier);
    }

    private static DataResult<StateShapedRecipe> decode(Definition definition) {
        try {
            return DataResult.success(new StateShapedRecipe(definition));
        } catch (IllegalArgumentException | IllegalStateException error) {
            return DataResult.error(error::getMessage);
        }
    }

    private int orientation(CraftingInput input) {
        if (input.width() != getWidth() || input.height() != getHeight()) return -1;
        for (int flip = 0; flip <= 1; flip++) {
            boolean matched = true;
            for (int slot = 0; slot < input.size(); slot++) {
                InputStateRule.Bound expected = cells[sourceCell(slot, flip)];
                ItemStack stack = input.getItem(slot);
                if (expected == null ? !stack.isEmpty() : !expected.acceptsItem(stack)) {
                    matched = false;
                    break;
                }
            }
            if (matched) return flip;
        }
        return -1;
    }

    private int sourceCell(int slot, int flip) {
        int width = getWidth();
        int x = slot % width;
        return (flip == 0 ? x : width - 1 - x) + slot / width * width;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.width() == getWidth()
                && input.height() == getHeight()
                && input.ingredientCount() == ingredientCount
                && RecipeInputState.crafting(this, input, null, true);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return assemble(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (carrier == -1) return result.create();
        int flip = orientation(input);
        if (flip < 0) return ItemStack.EMPTY;
        return TransmuteRecipe.createWithOriginalComponents(
                result, input.getItem(sourceCell(carrier, flip)));
    }

    @Override
    public boolean permitsInputStates(
            CraftingInput input, @Nullable ItemStack output, boolean returnsRemainders) {
        int flip = orientation(input);
        if (flip < 0) return false;
        if (carrier == -1 && returnsRemainders) return true;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || stack.getComponentsPatch().isEmpty()) continue;
            InputStateRule.Bound bound = cells[sourceCell(slot, flip)];
            if (bound.preserve() == InputStateRule.Preservation.RESULT && output == null)
                output = TransmuteRecipe.createWithOriginalComponents(result, stack);
            if (!bound.permitsMatched(
                    stack, output == null ? ItemStack.EMPTY : output, returnsRemainders))
                return false;
        }
        return true;
    }

    // backport: 26.x recipe-book display() dropped; the 1.21.1 recipe book and JEI read the
    // pattern (getIngredients(), getWidth()/getHeight()) and getResultItem() of ShapedRecipe.

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }

    private record Definition(
            NormalCraftingRecipe.CommonInfo commonInfo,
            NormalCraftingRecipe.CraftingBookInfo bookInfo,
            ShapedRecipePattern.Data pattern,
            ItemStackTemplate result,
            Map<String, InputStateRule> inputState) {}
}
