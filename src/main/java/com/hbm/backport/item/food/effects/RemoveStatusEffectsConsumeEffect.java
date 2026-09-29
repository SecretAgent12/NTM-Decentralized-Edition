// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 26.x {@code consume_effects.RemoveStatusEffectsConsumeEffect}. */
public record RemoveStatusEffectsConsumeEffect(HolderSet<MobEffect> effects) implements ConsumeEffect {

    public static final MapCodec<RemoveStatusEffectsConsumeEffect> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT)
                                                    .fieldOf("effects")
                                                    .forGetter(RemoveStatusEffectsConsumeEffect::effects))
                                    .apply(i, RemoveStatusEffectsConsumeEffect::new));
    public static final Type<RemoveStatusEffectsConsumeEffect> TYPE = new Type<>("remove_effects", CODEC);

    public RemoveStatusEffectsConsumeEffect(Holder<MobEffect> effect) {
        this(HolderSet.direct(effect));
    }

    @Override
    public Type<RemoveStatusEffectsConsumeEffect> getType() {
        return TYPE;
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity entity) {
        boolean removed = false;
        for (Holder<MobEffect> effect : effects) {
            if (entity.removeEffect(effect)) removed = true;
        }
        return removed;
    }
}
