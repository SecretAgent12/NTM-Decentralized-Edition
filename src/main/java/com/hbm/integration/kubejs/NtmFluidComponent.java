// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.kubejs;

import com.google.gson.JsonElement;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.fluid.FluidWrapper;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.UniqueIdBuilder;
import dev.latvian.mods.kubejs.recipe.filter.RecipeMatchContext;
import dev.latvian.mods.kubejs.recipe.match.FluidMatch;
import dev.latvian.mods.kubejs.recipe.match.ReplacementMatchInfo;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One NTM recipe fluid ({@code {type, amount, pressure}}), read and written with NTM's own codec so
 * the pressure survives. From scripts: {@code Fluid.of('hbm:sulfuric_acid', 1000)}, a fluid id
 * (1000 mB), or an object in NTM's JSON form ({@code {type: 'hbm:x', amount: 500, pressure: 1}}).
 * A replaced fluid keeps the original amount and pressure.
 */
public record NtmFluidComponent(RecipeComponentType<?> type) implements RecipeComponent<FluidStackNTM> {

    private static final TypeInfo TYPE_INFO = TypeInfo.of(FluidStackNTM.class);

    @Override
    public Codec<FluidStackNTM> codec() {
        return FluidStackNTM.CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TYPE_INFO;
    }

    @Override
    public FluidStackNTM wrap(RecipeScriptContext cx, Object from) {
        if (from instanceof FluidStackNTM stack) return stack;
        if (from instanceof Map<?, ?> map && map.containsKey("type")) {
            JsonElement json = JsonUtils.of(cx.cx(), from);
            return codec().parse(cx.ops().json(), json).getOrThrow();
        }
        FluidStack stack = FluidWrapper.wrap(cx.cx(), from);
        return new FluidStackNTM(stack.getFluid(), stack.getAmount());
    }

    private static FluidStack vanilla(FluidStackNTM value) {
        if (isBlank(value)) return FluidStack.EMPTY;
        return new FluidStack(value.type(), (int) Math.min(value.amount(), Integer.MAX_VALUE));
    }

    private static boolean isBlank(FluidStackNTM value) {
        return value.amount() <= 0 || value.type() == Fluids.EMPTY;
    }

    // NTM data uses blank rows on purpose (SILEX fluids with amount 0, a minecraft:empty second
    // output on cracking/radiolysis), so blank values are valid here
    @Override
    public boolean allowEmpty() {
        return true;
    }

    @Override
    public boolean isEmpty(FluidStackNTM value) {
        return isBlank(value);
    }

    @Override
    public boolean matches(RecipeMatchContext cx, FluidStackNTM value, ReplacementMatchInfo match) {
        return !isBlank(value)
                && match.match() instanceof FluidMatch m
                && m.matches(cx, vanilla(value), match.exact());
    }

    @Override
    public FluidStackNTM replace(
            RecipeScriptContext cx, FluidStackNTM original, ReplacementMatchInfo match, Object with) {
        if (!matches(cx, original, match)) return original;
        FluidStack replacement = FluidWrapper.wrap(cx.cx(), with);
        return new FluidStackNTM(replacement.getFluid(), original.amount(), original.pressure());
    }

    @Override
    public void buildUniqueId(UniqueIdBuilder builder, FluidStackNTM value) {
        builder.append(BuiltInRegistries.FLUID.getKey(value.type()));
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
