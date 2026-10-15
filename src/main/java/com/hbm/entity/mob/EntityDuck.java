// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.ModEntities;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.ChickenCompat;

public class EntityDuck extends ChickenCompat {

    public EntityDuck(EntityType<? extends EntityDuck> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_DUCC.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_DUCC.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_DUCC.get();
    }

    @Override
    public @Nullable Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.DUCK.get().create(level);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) {
            server.getServer()
                    .getPlayerList()
                    .broadcastSystemMessage(getCombatTracker().getDeathMessage(), false);
        }

        super.die(source);
    }
}
