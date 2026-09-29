// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.NuclearTech;
import com.hbm.lib.Library;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public record HbmNarrowedIngredient(Holder<Item> item, Ingredient within)
        implements ICustomIngredient {

    public static final ResourceLocation ID = Library.id("narrowed");

    private static final MapCodec<HbmNarrowedIngredient> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            ItemStack.ITEM_NON_AIR_CODEC
                                                    .fieldOf("item")
                                                    .forGetter(HbmNarrowedIngredient::item),
                                            Ingredient.CODEC
                                                    .fieldOf("within")
                                                    .forGetter(HbmNarrowedIngredient::within))
                                    .apply(i, HbmNarrowedIngredient::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, HbmNarrowedIngredient> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.holderRegistry(Registries.ITEM),
                    HbmNarrowedIngredient::item,
                    Ingredient.CONTENTS_STREAM_CODEC,
                    HbmNarrowedIngredient::within,
                    HbmNarrowedIngredient::new);

    public static Ingredient of(Holder<Item> item, Ingredient within) {
        return new HbmNarrowedIngredient(item, within).toVanilla();
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(item) && within.test(stack);
    }

    // backport: 1.21.1 custom ingredients have one getItems() for display and lookup
    // instead of 26.x's items() + display(); it returns what display() showed.
    @Override
    public Stream<ItemStack> getItems() {
        return CountIngredient.displayStacks(within, 1).stream().filter(stack -> stack.is(item));
    }

    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, NuclearTech.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<HbmNarrowedIngredient>>
            TYPE =
                    INGREDIENT_TYPES.register(
                            ID.getPath(), () -> new IngredientType<>(CODEC, STREAM_CODEC));

    public static void subscribe(IEventBus modBus) {
        INGREDIENT_TYPES.register(modBus);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }
}
