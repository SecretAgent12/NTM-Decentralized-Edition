// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

/** 26.x {@code consume_effects.TeleportRandomlyConsumeEffect} (chorus fruit), after 1.21.1 ChorusFruitItem. */
public record TeleportRandomlyConsumeEffect(float diameter) implements ConsumeEffect {

    public static final MapCodec<TeleportRandomlyConsumeEffect> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            ExtraCodecs.POSITIVE_FLOAT
                                                    .optionalFieldOf("diameter", 16.0F)
                                                    .forGetter(TeleportRandomlyConsumeEffect::diameter))
                                    .apply(i, TeleportRandomlyConsumeEffect::new));
    public static final Type<TeleportRandomlyConsumeEffect> TYPE = new Type<>("teleport_randomly", CODEC);

    public TeleportRandomlyConsumeEffect() {
        this(16.0F);
    }

    @Override
    public Type<TeleportRandomlyConsumeEffect> getType() {
        return TYPE;
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity entity) {
        if (!(level instanceof ServerLevel server)) return false;
        boolean teleported = false;
        for (int i = 0; i < 16; i++) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * diameter;
            double y =
                    Mth.clamp(
                            entity.getY() + (entity.getRandom().nextDouble() - 0.5) * diameter,
                            level.getMinBuildHeight(),
                            level.getMinBuildHeight() + server.getLogicalHeight() - 1);
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * diameter;
            if (entity.isPassenger()) entity.stopRiding();
            Vec3 from = entity.position();
            EntityTeleportEvent.ChorusFruit event = EventHooks.onChorusFruitTeleport(entity, x, y, z);
            if (event.isCanceled()) return false;
            if (entity.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) {
                level.gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(entity));
                SoundEvent sound;
                SoundSource source;
                if (entity instanceof Fox) {
                    sound = SoundEvents.FOX_TELEPORT;
                    source = SoundSource.NEUTRAL;
                } else {
                    sound = SoundEvents.CHORUS_FRUIT_TELEPORT;
                    source = SoundSource.PLAYERS;
                }
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, source);
                entity.resetFallDistance();
                teleported = true;
                break;
            }
        }
        if (teleported && entity instanceof Player player) player.resetCurrentImpulseContext();
        return teleported;
    }
}
