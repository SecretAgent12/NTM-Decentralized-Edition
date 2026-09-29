// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.hbm.backport.compat.ItemCompat;

public final class ItemRepairKit extends ItemCompat {

    private final Supplier<SoundEvent> sound;

    public ItemRepairKit(Properties properties, Supplier<SoundEvent> sound) {
        super(properties);
        this.sound = sound;
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean didSomething = false;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack gunStack = player.getInventory().getItem(slot);
            if (!(gunStack.getItem() instanceof ItemGunBaseNT gun)) continue;

            for (int index = 0; index < gun.getConfigCount(); index++) {
                float maxDurability = gun.getConfig(gunStack, index).getDurability(gunStack);

                if (Math.min(ItemGunBaseNT.getWear(gunStack, index), maxDurability) > 0) {
                    ItemGunBaseNT.setWear(
                            gunStack,
                            index,
                            Math.max(
                                    0F,
                                    ItemGunBaseNT.getWear(gunStack, index)
                                            - maxDurability * 0.25F));
                    didSomething = true;
                }
            }
        }

        ItemStack stack = player.getItemInHand(hand);
        if (didSomething) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    sound.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            stack.hurtAndBreak(1, player, (hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND));
        }
        return InteractionResult.SUCCESS;
    }
}
