// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.items.tool.ItemChainsaw;
import com.hbm.items.weapon.ItemCrucible;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Shadow private ItemStack mainHandItem;
    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;

    private static final String ARM_WITH_ITEM =
            "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    /**
     * backport: 26.x swaps the item's SwingAnimation for NONE (no first-person swing bob). 1.21.1 has
     * no swing-animation component: the swing bob of renderArmWithItem is driven only by its
     * swingProgress argument (float argument #2: partialTicks, pitch, swingProgress,
     * equippedProgress), so the chainsaw and crucible get a swing progress of 0.
     */
    @ModifyVariable(method = ARM_WITH_ITEM, at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float hbm$toolFirstPersonSwing(
            float swingProgress, @Local(argsOnly = true) ItemStack itemStack) {
        return itemStack.getItem() instanceof ItemChainsaw
                        || itemStack.getItem() instanceof ItemCrucible
                ? 0.0F
                : swingProgress;
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void hbm$crucibleEquipProgress(CallbackInfo ci) {
        if (!(mainHandItem.getItem() instanceof ItemCrucible)) return;
        if (mainHandHeight < oMainHandHeight) mainHandHeight = oMainHandHeight = 0;
        else if (mainHandHeight > oMainHandHeight) mainHandHeight = oMainHandHeight = 1;
    }
}
