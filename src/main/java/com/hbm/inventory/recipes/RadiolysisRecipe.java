// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.hbm.backport.RecipeSerializers;

public class RadiolysisRecipe extends GenericRecipe {

    public static final MapCodec<RadiolysisRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(RadiolysisRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, RadiolysisRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(RadiolysisRecipe::new);
    public static final RecipeSerializer<RadiolysisRecipe> SERIALIZER =
            RecipeSerializers.of(MAP_CODEC, STREAM_CODEC);

    public RadiolysisRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return RadiolysisRecipes.INSTANCE;
    }
}
