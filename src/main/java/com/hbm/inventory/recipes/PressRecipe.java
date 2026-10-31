// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemStamp.StampType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.hbm.backport.RecipeSerializers;
import org.jspecify.annotations.Nullable;

public class PressRecipe extends GenericRecipe {

    // backport-fix: BF-128 (GitHub #47) the stamp is a type name, not a closed enum: NTM's own
    // names as before ("flat", "plate", ...) plus any name a datapack/KubeJS gives items through
    // the item tag hbm:stamps/<name> (see StampTypes)
    private static final Codec<String> STAMP_TYPE_CODEC =
            Codec.STRING.comapFlatMap(
                    name -> {
                        String type = name.toLowerCase(Locale.ROOT);
                        return ResourceLocation.isValidPath(type)
                                ? DataResult.success(type)
                                : DataResult.error(() -> "Invalid stamp type '" + name + "'");
                    },
                    type -> type);
    private static final MapCodec<String> STAMP_MAP_CODEC = STAMP_TYPE_CODEC.fieldOf("stamp");
    private static final StreamCodec<RegistryFriendlyByteBuf, String> STAMP_STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.cast();
    private static final BiConsumer<PressRecipe, String> APPLY_STAMP =
            (recipe, stampType) -> recipe.stampType = stampType;
    private static final Function<PressRecipe, String> EXTRACT_STAMP =
            recipe -> recipe.stampType;
    public static final MapCodec<PressRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(PressRecipe::new, STAMP_MAP_CODEC, APPLY_STAMP, EXTRACT_STAMP);
    public static final StreamCodec<RegistryFriendlyByteBuf, PressRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(
                    PressRecipe::new, STAMP_STREAM_CODEC, APPLY_STAMP, EXTRACT_STAMP);
    public static final RecipeSerializer<PressRecipe> SERIALIZER =
            RecipeSerializers.of(MAP_CODEC, STREAM_CODEC);
    public String stampType;

    public PressRecipe(String name) {
        super(name);
    }

    public PressRecipe setStampType(StampType stampType) {
        this.stampType = StampTypes.name(stampType);
        return this;
    }

    // backport-fix: BF-128 the stamp type name; type() stays for NTM's own types (null for others)
    public String stamp() {
        return stampType;
    }

    public @Nullable StampType type() {
        for (StampType builtIn : StampType.values())
            if (StampTypes.name(builtIn).equals(stampType)) return builtIn;
        return null;
    }

    public CountIngredient input() {
        return inputItem[0];
    }

    public ItemStack output() {
        return outputItems()[0].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return PressRecipes.INSTANCE;
    }
}
