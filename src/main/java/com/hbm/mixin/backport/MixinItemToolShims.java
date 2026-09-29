// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.item.tool.Enchantable;
import com.hbm.backport.item.tool.Repairable;
import com.hbm.backport.item.tool.ToolRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the backport's stand-ins for 26.x item components effective on 1.21.1 {@link Item}:
 * hbm:backport_enchantable -> getEnchantmentValue / isEnchantable, hbm:backport_repairable ->
 * isValidRepairItem, hbm:backport_no_creative_destroy -> canAttackBlock. Each hook only acts
 * when its component is present; otherwise vanilla runs. Subclasses that override these methods
 * natively (ArmorItem, TieredItem, SwordItem, ...) keep their own answer.
 */
@Mixin(Item.class)
public abstract class MixinItemToolShims {

    @Inject(method = "getEnchantmentValue()I", at = @At("HEAD"), cancellable = true)
    private void backport$enchantable(CallbackInfoReturnable<Integer> cir) {
        Enchantable e = ToolRegistry.get(((Item) (Object) this).components(), ToolRegistry.ENCHANTABLE);
        if (e != null) cir.setReturnValue(e.value());
    }

    // 26.x: an item is enchantable (table) iff it has the ENCHANTABLE component
    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void backport$isEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (ToolRegistry.get(stack, ToolRegistry.ENCHANTABLE) != null) cir.setReturnValue(true);
    }

    @Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
    private void backport$repairable(
            ItemStack stack, ItemStack repairItem, CallbackInfoReturnable<Boolean> cir) {
        Repairable r = ToolRegistry.get(stack, ToolRegistry.REPAIRABLE);
        if (r != null) cir.setReturnValue(r.isValidRepairItem(repairItem));
    }

    // 26.x Tool.canDestroyBlocksInCreative == false (swords)
    @Inject(method = "canAttackBlock", at = @At("HEAD"), cancellable = true)
    private void backport$noCreativeDestroy(
            BlockState state, Level level, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.isCreative()
                && ToolRegistry.get(player.getMainHandItem(), ToolRegistry.NO_CREATIVE_DESTROY) != null) {
            cir.setReturnValue(false);
        }
    }
}
