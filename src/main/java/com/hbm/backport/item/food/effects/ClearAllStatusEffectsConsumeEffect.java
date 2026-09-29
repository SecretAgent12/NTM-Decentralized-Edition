// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food.effects;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.EffectCures;

/**
 * 26.x {@code consume_effects.ClearAllStatusEffectsConsumeEffect} (milk).
 * backport: cures like 1.21.1 NeoForge milk ({@code removeEffectsCuredBy(EffectCures.MILK)}); the vanilla milk
 * bucket itself stays a 1.21.1 MilkBucketItem and does not use this class.
 */
public record ClearAllStatusEffectsConsumeEffect() implements ConsumeEffect {

    public static final ClearAllStatusEffectsConsumeEffect INSTANCE = new ClearAllStatusEffectsConsumeEffect();
    public static final MapCodec<ClearAllStatusEffectsConsumeEffect> CODEC = MapCodec.unit(INSTANCE);
    public static final Type<ClearAllStatusEffectsConsumeEffect> TYPE = new Type<>("clear_all_effects", CODEC);

    @Override
    public Type<ClearAllStatusEffectsConsumeEffect> getType() {
        return TYPE;
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity entity) {
        return entity.removeEffectsCuredBy(EffectCures.MILK);
    }
}
