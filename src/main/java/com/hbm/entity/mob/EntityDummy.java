// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.item.armor.Equippable;
import net.minecraft.world.level.Level;
import com.hbm.backport.compat.MobCompat;

public class EntityDummy extends MobCompat {

    public EntityDummy(EntityType<? extends EntityDummy> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    protected InteractionResult backport$mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        Equippable equippable = com.hbm.backport.item.armor.Equippable.get(held);

        if (equippable != null) setItemSlot(equippable.slot(), held.copy());

        return super.backport$mobInteract(player, hand);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public Component getName() {
        return Component.literal(
                (int) (getHealth() * 10) / 10F + " / " + (int) (getMaxHealth() * 10) / 10F);
    }

    @Override
    protected void dropEquipment() {}
}
