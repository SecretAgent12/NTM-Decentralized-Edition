// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityItemBuoyant;
import com.hbm.items.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.SquidCompat;

public class EntityPlasticBag extends SquidCompat {

    public EntityPlasticBag(EntityType<? extends EntityPlasticBag> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Squid.createAttributes();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.45F, 0.45F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        discard();
        spawnAtLocation(new ItemStack(ModItems.PLASTIC_BAG), 0F);
        return true;
    }

    @Override
    public @Nullable ItemEntity spawnAtLocation(ItemStack stack, float offset) {
        if (!(level() instanceof ServerLevel level)) return null; // backport: 26.x passed the ServerLevel
        if (stack.isEmpty()) return null;

        EntityItemBuoyant item =
                new EntityItemBuoyant(level, getX(), getY() + offset, getZ(), stack);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
        return item;
    }

    // backport: getBreedOffspring dropped -- 1.21.1 Squid is not ageable

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        return getY() > 45.0D
                && getY() < 63.0D
                && random.nextInt(10) == 0
                && super.checkSpawnRules(level, reason);
    }
}
