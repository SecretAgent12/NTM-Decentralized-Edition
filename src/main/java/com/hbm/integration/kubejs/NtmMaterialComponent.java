// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.kubejs;

import com.google.gson.JsonElement;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.UniqueIdBuilder;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;
import java.util.Locale;
import java.util.Map;

/**
 * One foundry material amount ({@code {material, amount}}), as the rotary furnace and the crucible
 * use them. Amounts are in quanta: 8 = nugget, 72 = ingot, 648 = block. From scripts: an object in
 * NTM's JSON form ({@code {material: 'aluminum', amount: 144}}), {@code '144x aluminum'}, or just
 * {@code 'aluminum'} for one ingot (72).
 */
public record NtmMaterialComponent(RecipeComponentType<?> type)
        implements RecipeComponent<MaterialStack> {

    public static final int INGOT = 72;

    private static final TypeInfo TYPE_INFO = TypeInfo.of(MaterialStack.class);

    @Override
    public Codec<MaterialStack> codec() {
        return MaterialStack.CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TYPE_INFO;
    }

    @Override
    public MaterialStack wrap(RecipeScriptContext cx, Object from) {
        if (from instanceof MaterialStack stack) return stack;
        if (from instanceof Map<?, ?> || from instanceof JsonElement) {
            JsonElement json = JsonUtils.of(cx.cx(), from);
            return codec().parse(cx.ops().json(), json).getOrThrow();
        }
        if (from instanceof CharSequence text) return parse(text.toString());
        throw new IllegalArgumentException(
                "Expected an NTM material: {material, amount}, '144x name' or 'name', got " + from);
    }

    private static MaterialStack parse(String text) {
        String s = text.trim();
        int amount = INGOT;
        int x = s.indexOf('x');
        if (x > 0 && s.substring(0, x).trim().chars().allMatch(Character::isDigit)) {
            amount = Integer.parseInt(s.substring(0, x).trim());
            s = s.substring(x + 1).trim();
        }
        NTMMaterial material = Mats.matByName.get(s.toLowerCase(Locale.ROOT));
        if (material == null) throw new IllegalArgumentException("Unknown NTM material: " + s);
        return new MaterialStack(material, amount);
    }

    @Override
    public void buildUniqueId(UniqueIdBuilder builder, MaterialStack value) {
        builder.append(value.material.tagPath);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
