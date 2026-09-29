// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// backport: 26.x tryExtractRenderState (world block entities) is 1.21.1's
// BlockEntityRenderDispatcher.render(blockEntity, partialTick, poseStack, buffers); a removed block
// entity (e.g. one the client keeps for a predicted multiblock break) draws nothing
@Mixin(BlockEntityRenderDispatcher.class)
public class MixinRemovedBlockEntity {

    @Inject(
            method =
                    "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$skipRemoved(
            BlockEntity entity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            CallbackInfo ci) {
        if (entity.isRemoved()) ci.cancel();
    }
}
