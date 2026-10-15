// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.block.BlockPlacements;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemUsedOnLocationTrigger.class)
public abstract class MixinItemUsedOnLocationTrigger {
    // backport: 1.21.1 trigger takes the ItemStack (26.x: ItemInstance), package advancements.critereon
    @Inject(
            method =
                    "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"))
    private void hbm$playerPlaced(
            ServerPlayer player, BlockPos pos, ItemStack tool, CallbackInfo ci) {
        if ((Object) this == CriteriaTriggers.PLACED_BLOCK
                && !(tool.getItem() instanceof BucketItem)) {
            BlockPlacements.placedBy(player, pos);
        }
    }
}
