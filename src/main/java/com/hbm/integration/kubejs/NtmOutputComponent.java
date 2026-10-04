// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.backport.ItemStackTemplate;
import com.hbm.backport.random.Weighted;
import com.hbm.backport.random.WeightedList;
import com.hbm.inventory.recipes.loader.Outputs;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.UniqueIdBuilder;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.ItemMatch;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * One NTM recipe output: a plain item, an item with a chance ({@code {id, count, chance}}) or a
 * weighted pool ({@code [{data: {...}, weight}, ...]}), read and written with NTM's own codec.
 *
 * <p>From scripts: {@code 'hbm:ingot_steel'}, {@code '2x hbm:ingot_steel'}, an ItemStack, or an
 * object in NTM's JSON form ({@code {id: 'hbm:x', count: 2, chance: 0.5}}; {@code item} is accepted
 * in place of {@code id}).
 */
public record NtmOutputComponent(RecipeComponentType<?> type)
        implements RecipeComponent<WeightedList<Optional<ItemStackTemplate>>> {

    private static final TypeInfo TYPE_INFO = TypeInfo.of(WeightedList.class);

    @Override
    public Codec<WeightedList<Optional<ItemStackTemplate>>> codec() {
        return Outputs.TEMPLATE_CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TYPE_INFO;
    }

    @Override
    @SuppressWarnings("unchecked")
    public WeightedList<Optional<ItemStackTemplate>> wrap(RecipeScriptContext cx, Object from) {
        if (from instanceof WeightedList<?> list) {
            return (WeightedList<Optional<ItemStackTemplate>>) list;
        }
        if (from instanceof CharSequence || from instanceof ItemStack || from instanceof ItemLike) {
            return single(ItemWrapper.wrap(cx.cx(), from));
        }
        JsonElement json = JsonUtils.of(cx.cx(), from);
        if (json instanceof JsonObject obj && !obj.has("id") && obj.has("item")) {
            obj.add("id", obj.remove("item"));
        }
        return codec().parse(cx.ops().json(), json).getOrThrow();
    }

    private static WeightedList<Optional<ItemStackTemplate>> single(ItemStack stack) {
        return WeightedList.of(Optional.of(ItemStackTemplate.fromNonEmptyStack(stack)));
    }

    @Override
    public boolean isEmpty(WeightedList<Optional<ItemStackTemplate>> value) {
        return value.isEmpty();
    }

    @Override
    public boolean matches(
            RecipeMatchContext cx,
            WeightedList<Optional<ItemStackTemplate>> value,
            ReplacementMatchInfo match) {
        if (!(match.match() instanceof ItemMatch m)) return false;
        for (Weighted<Optional<ItemStackTemplate>> entry : value.unwrap()) {
            if (entry.value().isPresent()
                    && m.matches(cx, entry.value().get().create(), match.exact())) return true;
        }
        return false;
    }

    @Override
    public WeightedList<Optional<ItemStackTemplate>> replace(
            RecipeScriptContext cx,
            WeightedList<Optional<ItemStackTemplate>> original,
            ReplacementMatchInfo match,
            Object with) {
        if (!(match.match() instanceof ItemMatch m)) return original;
        ItemStack replacement = ItemWrapper.wrap(cx.cx(), with);
        boolean changed = false;
        List<Weighted<Optional<ItemStackTemplate>>> out = new ArrayList<>();
        for (Weighted<Optional<ItemStackTemplate>> entry : original.unwrap()) {
            Optional<ItemStackTemplate> value = entry.value();
            if (value.isPresent() && m.matches(cx, value.get().create(), match.exact())) {
                // keep the original amount unless the replacement names one
                int count = replacement.getCount() > 1 ? replacement.getCount() : value.get().count();
                value = Optional.of(ItemStackTemplate.fromNonEmptyStack(replacement.copyWithCount(count)));
                changed = true;
            }
            out.add(new Weighted<>(value, entry.weight()));
        }
        return changed ? WeightedList.of(out) : original;
    }

    @Override
    public void buildUniqueId(UniqueIdBuilder builder, WeightedList<Optional<ItemStackTemplate>> value) {
        for (Weighted<Optional<ItemStackTemplate>> entry : value.unwrap()) {
            if (entry.value().isPresent()) {
                builder.append(BuiltInRegistries.ITEM.getKey(entry.value().get().getItem()));
                return;
            }
        }
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
