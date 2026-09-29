// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.client.VanishedEntities;
import com.hbm.client.render.VanishedRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 1.21.1 has no vanilla EntityRenderState; this targets the core shim of the 26.x class.
 * 26.x set the vanished flag in EntityRenderer#createRenderState (MixinEntityRenderer); in 1.21.1
 * every extraction goes through the shim's static extractEntity, so the flag is set there.
 */
@Mixin(EntityRenderState.class)
public abstract class MixinEntityRenderState implements VanishedRenderState {
    @Unique private boolean hbm$vanished;

    @Override
    public boolean hbm$vanished() {
        return hbm$vanished;
    }

    @Override
    public void hbm$setVanished(boolean vanished) {
        hbm$vanished = vanished;
    }

    @Inject(
            method =
                    "extractEntity(Lnet/minecraft/world/entity/Entity;Lcom/hbm/backport/client/core/EntityRenderState;FI)V",
            at = @At("RETURN"),
            remap = false)
    private static void hbm$extract(
            Entity entity,
            EntityRenderState state,
            float partialTicks,
            int lightCoords,
            CallbackInfo ci) {
        ((VanishedRenderState) state).hbm$setVanished(VanishedEntities.isVanished(entity));
    }
}
