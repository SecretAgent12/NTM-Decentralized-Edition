// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food;

import com.hbm.backport.item.food.effects.ConsumeEffect;
import com.hbm.backport.item.food.effects.ConsumeEffects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * 26.x {@code net.minecraft.world.item.component.Consumable} (the {@code consumable} item component) on 1.21.1.
 *
 * <p>Stored as the backport data component {@link ConsumableRegistry#CONSUMABLE}
 * ({@code hbm:backport_consumable}); the behaviour 26.x wires into {@code Item} / {@code ItemStack} /
 * {@code LivingEntity} comes from the backport mixins {@code MixinBackportConsumableItem} (use, finishUsingItem,
 * getUseDuration, getUseAnimation) and {@code MixinBackportConsumableLiving} (sounds and particles while consuming).
 */
public record Consumable(
        float consumeSeconds,
        UseAnim animation,
        Holder<SoundEvent> sound,
        boolean hasConsumeParticles,
        List<ConsumeEffect> onConsumeEffects) {

    public static final float DEFAULT_CONSUME_SECONDS = 1.6F;
    private static final float PARTICLE_EMISSION_DURATION_FRACTION = 0.21875F;

    // backport: 1.21.1 UseAnim is not StringRepresentable; the lower-case enum names match 26.x ItemUseAnimation ids
    public static final Codec<UseAnim> ANIMATION_CODEC =
            Codec.STRING.comapFlatMap(
                    s -> {
                        // unknown ids (26.x-only animations such as "bundle") fail softly
                        for (UseAnim a : UseAnim.values())
                            if (a.name().equalsIgnoreCase(s)) return com.mojang.serialization.DataResult.success(a);
                        return com.mojang.serialization.DataResult.error(() -> "Unknown use animation: " + s);
                    },
                    a -> a.name().toLowerCase(Locale.ROOT));

    public static final Codec<Consumable> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.floatRange(0.0F, Float.MAX_VALUE)
                                                    .optionalFieldOf("consume_seconds", DEFAULT_CONSUME_SECONDS)
                                                    .forGetter(Consumable::consumeSeconds),
                                            ANIMATION_CODEC
                                                    .optionalFieldOf("animation", UseAnim.EAT)
                                                    .forGetter(Consumable::animation),
                                            SoundEvent.CODEC
                                                    .optionalFieldOf("sound", holder(SoundEvents.GENERIC_EAT))
                                                    .forGetter(Consumable::sound),
                                            Codec.BOOL
                                                    .optionalFieldOf("has_consume_particles", true)
                                                    .forGetter(Consumable::hasConsumeParticles),
                                            ConsumeEffects.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("on_consume_effects", List.of())
                                                    .forGetter(Consumable::onConsumeEffects))
                                    .apply(i, Consumable::new));

    public Consumable {
        onConsumeEffects = List.copyOf(onConsumeEffects);
    }

    public static Builder builder() {
        return new Builder();
    }

    static Holder<SoundEvent> holder(SoundEvent sound) {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
    }

    public int consumeTicks() {
        return (int) (consumeSeconds * 20.0F);
    }

    /** 26.x Consumable.startConsuming; returns the 1.21.1 use() result. */
    public InteractionResultHolder<ItemStack> startConsuming(
            LivingEntity user, ItemStack stack, InteractionHand hand) {
        if (!canConsume(user, stack)) return InteractionResultHolder.fail(stack);
        if (consumeTicks() > 0) {
            user.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        // instant consumption: 26.x ItemStack.use also applies use_remainder here
        ItemStack before = stack.copy();
        ItemStack result = onConsume(user.level(), user, stack);
        return InteractionResultHolder.consume(UseRemainder.applyAfterUse(before, result, user));
    }

    public boolean canConsume(LivingEntity user, ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food != null && user instanceof Player player ? player.canEat(food.canAlwaysEat()) : true;
    }

    /** 26.x Consumable.onConsume: effects of finishing to eat/drink; shrinks the stack by one. */
    public ItemStack onConsume(Level level, LivingEntity user, ItemStack stack) {
        RandomSource random = user.getRandom();
        emitParticlesAndSounds(random, user, stack, 16);
        if (user instanceof ServerPlayer player) {
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
        }

        // 26.x ConsumableListener: FoodProperties is the only listener the mod's items carry
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            level.playSound(
                    null,
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    sound.value(),
                    SoundSource.NEUTRAL,
                    1.0F,
                    (float) random.triangle(1.0, 0.4));
            if (user instanceof Player player) {
                player.getFoodData().eat(food);
                level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.PLAYER_BURP,
                        SoundSource.PLAYERS,
                        0.5F,
                        Mth.randomBetween(random, 0.9F, 1.0F));
            }
            // backport: 1.21.1 FoodProperties.effects()/usingConvertsTo() are not applied here (26.x FoodProperties
            // has neither; the mod builds foods with the 26.x-shaped builder only)
        }

        if (!level.isClientSide()) {
            for (ConsumeEffect effect : onConsumeEffects) effect.apply(level, stack, user);
        }

        user.gameEvent(animation == UseAnim.DRINK ? GameEvent.DRINK : GameEvent.EAT);
        stack.consume(1, user);
        return stack;
    }

    public boolean shouldEmitParticlesAndSounds(int remainingUseDuration) {
        int elapsed = consumeTicks() - remainingUseDuration;
        int threshold = (int) (consumeTicks() * PARTICLE_EMISSION_DURATION_FRACTION);
        return elapsed > threshold && remainingUseDuration % 4 == 0;
    }

    public void emitParticlesAndSounds(RandomSource random, LivingEntity entity, ItemStack stack, int amount) {
        float eatVolume = random.nextBoolean() ? 0.5F : 1.0F;
        float eatPitch = (float) random.triangle(1.0, 0.2);
        float drinkPitch = Mth.randomBetween(random, 0.9F, 1.0F);
        float volume = animation == UseAnim.DRINK ? 0.5F : eatVolume;
        float pitch = animation == UseAnim.DRINK ? drinkPitch : eatPitch;
        if (hasConsumeParticles) spawnItemParticles(entity, stack, amount);
        // backport: 26.x Consumable.OverrideConsumeSound (entity-specific sounds) does not exist; always this.sound
        entity.playSound(sound.value(), volume, pitch);
    }

    // backport: LivingEntity.spawnItemParticles is private in 1.21.1; same code as there
    private static void spawnItemParticles(LivingEntity entity, ItemStack stack, int amount) {
        RandomSource random = entity.getRandom();
        for (int i = 0; i < amount; i++) {
            Vec3 speed = new Vec3((random.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
            speed = speed.xRot(-entity.getXRot() * (float) (Math.PI / 180.0));
            speed = speed.yRot(-entity.getYRot() * (float) (Math.PI / 180.0));
            double y = -random.nextFloat() * 0.6 - 0.3;
            Vec3 pos = new Vec3((random.nextFloat() - 0.5) * 0.3, y, 0.6);
            pos = pos.xRot(-entity.getXRot() * (float) (Math.PI / 180.0));
            pos = pos.yRot(-entity.getYRot() * (float) (Math.PI / 180.0));
            pos = pos.add(entity.getX(), entity.getEyeY(), entity.getZ());
            entity.level()
                    .addParticle(
                            new ItemParticleOption(ParticleTypes.ITEM, stack),
                            pos.x,
                            pos.y,
                            pos.z,
                            speed.x,
                            speed.y + 0.05,
                            speed.z);
        }
    }

    public static class Builder {
        private float consumeSeconds = DEFAULT_CONSUME_SECONDS;
        private UseAnim animation = UseAnim.EAT;
        private Holder<SoundEvent> sound = holder(SoundEvents.GENERIC_EAT);
        private boolean hasConsumeParticles = true;
        private final List<ConsumeEffect> onConsumeEffects = new ArrayList<>();

        Builder() {}

        public Builder consumeSeconds(float consumeSeconds) {
            this.consumeSeconds = consumeSeconds;
            return this;
        }

        public Builder animation(UseAnim animation) {
            this.animation = animation;
            return this;
        }

        public Builder sound(Holder<SoundEvent> sound) {
            this.sound = sound;
            return this;
        }

        // backport: 1.21.1 SoundEvents constants are plain SoundEvents, not holders
        public Builder sound(SoundEvent sound) {
            return sound(holder(sound));
        }

        public Builder soundAfterConsume(Holder<SoundEvent> sound) {
            return onConsume(new com.hbm.backport.item.food.effects.PlaySoundConsumeEffect(sound));
        }

        public Builder hasConsumeParticles(boolean hasConsumeParticles) {
            this.hasConsumeParticles = hasConsumeParticles;
            return this;
        }

        public Builder onConsume(ConsumeEffect effect) {
            this.onConsumeEffects.add(effect);
            return this;
        }

        public Consumable build() {
            return new Consumable(consumeSeconds, animation, sound, hasConsumeParticles, onConsumeEffects);
        }
    }
}
