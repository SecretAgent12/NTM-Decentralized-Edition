// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.x (1.21.2+) ingredient JSON: {@code "minecraft:stick"}, {@code "#c:ingots/iron"} or a list of
 * such strings. 1.21.1 (NeoForge 21.1) only reads {@code {"item": ..}} / {@code {"tag": ..}} objects
 * (or lists of them). All ntm-next recipes are written in the 26.x form, so the ingredient codec
 * accepts both: the 1.21.1 form first, then the string forms. Encoding is unchanged (1.21.1 form).
 */
@Mixin(value = CraftingHelper.class, remap = false)
public abstract class IngredientStringFormMixin {

    @ModifyReturnValue(method = "makeIngredientCodec", at = @At("RETURN"))
    private static Codec<Ingredient> hbm$stringForms(Codec<Ingredient> original, boolean allowEmpty) {
        Codec<Ingredient> strings = Codec.either(Codec.STRING, Codec.STRING.listOf())
                .comapFlatMap(
                        e -> e.map(s -> parse(List.of(s), allowEmpty), l -> parse(l, allowEmpty)),
                        i -> Either.right(List.of()));
        return Codec.either(original, strings)
                .xmap(e -> e.map(i -> i, i -> i), Either::left);
    }

    private static DataResult<Ingredient> parse(List<String> entries, boolean allowEmpty) {
        List<Ingredient.Value> values = new ArrayList<>();
        for (String s : entries) {
            if (s.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(s.substring(1));
                if (id == null) return DataResult.error(() -> "Bad tag id: " + s);
                values.add(new Ingredient.TagValue(TagKey.create(Registries.ITEM, id)));
            } else {
                ResourceLocation id = ResourceLocation.tryParse(s);
                if (id == null) return DataResult.error(() -> "Bad item id: " + s);
                var item = BuiltInRegistries.ITEM.getOptional(id);
                if (item.isEmpty()) return DataResult.error(() -> "Unknown item: " + s);
                values.add(new Ingredient.ItemValue(new net.minecraft.world.item.ItemStack(item.get())));
            }
        }
        if (values.isEmpty() && !allowEmpty) return DataResult.error(() -> "Item array cannot be empty");
        return DataResult.success(Ingredient.fromValues(values.stream()));
    }
}
