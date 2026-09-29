// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 26.x {@code consume_effects.ApplyStatusEffectsConsumeEffect}. */
public record ApplyStatusEffectsConsumeEffect(List<MobEffectInstance> effects, float probability)
        implements ConsumeEffect {

    public static final MapCodec<ApplyStatusEffectsConsumeEffect> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            MobEffectInstance.CODEC
                                                    .listOf()
                                                    .fieldOf("effects")
                                                    .forGetter(ApplyStatusEffectsConsumeEffect::effects),
                                            Codec.floatRange(0.0F, 1.0F)
                                                    .optionalFieldOf("probability", 1.0F)
                                                    .forGetter(ApplyStatusEffectsConsumeEffect::probability))
                                    .apply(i, ApplyStatusEffectsConsumeEffect::new));
    public static final Type<ApplyStatusEffectsConsumeEffect> TYPE = new Type<>("apply_effects", CODEC);

    public ApplyStatusEffectsConsumeEffect {
        effects = List.copyOf(effects);
    }

    public ApplyStatusEffectsConsumeEffect(MobEffectInstance effect, float probability) {
        this(List.of(effect), probability);
    }

    public ApplyStatusEffectsConsumeEffect(List<MobEffectInstance> effects) {
        this(effects, 1.0F);
    }

    public ApplyStatusEffectsConsumeEffect(MobEffectInstance effect) {
        this(effect, 1.0F);
    }

    @Override
    public Type<ApplyStatusEffectsConsumeEffect> getType() {
        return TYPE;
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity entity) {
        if (entity.getRandom().nextFloat() >= probability) return false;
        boolean applied = false;
        for (MobEffectInstance effect : effects) {
            if (entity.addEffect(new MobEffectInstance(effect))) applied = true;
        }
        return applied;
    }
}
