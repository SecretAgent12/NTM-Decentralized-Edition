// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 26.x {@code consume_effects.PlaySoundConsumeEffect}. */
public record PlaySoundConsumeEffect(Holder<SoundEvent> sound) implements ConsumeEffect {

    public static final MapCodec<PlaySoundConsumeEffect> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(SoundEvent.CODEC.fieldOf("sound").forGetter(PlaySoundConsumeEffect::sound))
                                    .apply(i, PlaySoundConsumeEffect::new));
    public static final Type<PlaySoundConsumeEffect> TYPE = new Type<>("play_sound", CODEC);

    @Override
    public Type<PlaySoundConsumeEffect> getType() {
        return TYPE;
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity entity) {
        level.playSound(null, entity.blockPosition(), sound.value(), entity.getSoundSource(), 1.0F, 1.0F);
        return true;
    }
}
