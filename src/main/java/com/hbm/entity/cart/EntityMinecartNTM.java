// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.cart;

import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.backport.compat.AbstractMinecartCompat;

public abstract class EntityMinecartNTM extends AbstractMinecartCompat implements ICartChassis {
    // backport: 1.21.1 AbstractMinecart.getMinecartType() is abstract (gone in 26.x). NeoForge only reads it
    // for canBeRidden()/isPoweredCart() and vanilla for the pick item (overridden):
    // CHEST = neither rideable nor powered, like a 26.x AbstractMinecart subclass.
    @Override
    public net.minecraft.world.entity.vehicle.AbstractMinecart.Type getMinecartType() {
        return net.minecraft.world.entity.vehicle.AbstractMinecart.Type.CHEST;
    }


    private static final EntityDataAccessor<Integer> BASE =
            SynchedEntityData.defineId(EntityMinecartNTM.class, EntityDataSerializers.INT);

    protected EntityMinecartNTM(EntityType<? extends EntityMinecartNTM> type, Level level) {
        super(type, level);
    }

    protected EntityMinecartNTM(
            EntityType<? extends EntityMinecartNTM> type,
            Level level,
            double x,
            double y,
            double z,
            EnumCartBase base) {
        this(type, level);
        moveTo(x, y, z);
        setBase(base);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BASE, 0);
    }

    public void setBase(EnumCartBase type) {
        entityData.set(BASE, type.ordinal());
    }

    @Override
    public EnumCartBase getBase() {
        return EnumCartBase.values()[entityData.get(BASE)];
    }

    @Override
    protected Item getDropItem() {
        return getCartItem().getItem();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return getCartItem();
    }

    @Override
    public void destroy(DamageSource source) {
        if (!(level() instanceof ServerLevel level)) return; // backport: 26.x passed the ServerLevel
        kill();

        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack stack = getCartItem();
            if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());
            spawnAtLocation(stack);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("base", entityData.get(BASE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(BASE, input.getIntOr("base", 0));
    }
}
