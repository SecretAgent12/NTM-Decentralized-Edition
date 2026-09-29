// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food.effects;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 26.x {@code net.minecraft.world.item.consume_effects.ConsumeEffect}. Run server-side by
 * {@link com.hbm.backport.item.food.Consumable#onConsume}. backport: the effect types are a fixed set
 * ({@link ConsumeEffects}), not a registry.
 */
public interface ConsumeEffect {

    Type<? extends ConsumeEffect> getType();

    boolean apply(Level level, ItemStack stack, LivingEntity entity);

    record Type<T extends ConsumeEffect>(String id, MapCodec<T> codec) {}
}
