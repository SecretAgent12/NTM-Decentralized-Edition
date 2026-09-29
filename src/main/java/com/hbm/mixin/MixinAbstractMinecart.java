// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.IMinecartRail;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public class MixinAbstractMinecart {

    @ModifyReturnValue(method = "getMaxSpeed", at = @At("RETURN"))
    private double hbm$railMaxSpeed(double original) {
        IMinecartRail rail = hbm$railUnder();
        return rail == null ? original : original * (rail.railMaxSpeed() / 0.4F);
    }

    /**
     * backport: on 1.21.1 the on-rail speed cap is NeoForge's getMaxSpeedWithRail (the rail's
     * getRailMaxSpeed, 0.4 by default, capped by the cart's speed cap); getMaxSpeed only applies
     * off rails. Scale it the same way so NTM rails keep their 26.x speed.
     */
    @ModifyReturnValue(method = "getMaxSpeedWithRail", at = @At("RETURN"))
    private double hbm$railMaxSpeedOnRail(double original) {
        IMinecartRail rail = hbm$railUnder();
        return rail == null ? original : original * (rail.railMaxSpeed() / 0.4F);
    }

    /**
     * backport: 26.x runs this right after MinecartBehavior.tick() (the rail movement). 1.21.1
     * has no MinecartBehavior; the rail movement is the moveAlongTrack / comeOffTrack branch of
     * the server half of tick(), immediately followed by checkInsideBlocks().
     */
    @Inject(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;checkInsideBlocks()V"))
    private void hbm$railPass(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        if (!(self.level() instanceof ServerLevel)) return;
        IMinecartRail rail = hbm$railUnder();
        if (rail != null) rail.onMinecartPass(self);
    }

    @Unique
    private @Nullable IMinecartRail hbm$railUnder() {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        Level level = self.level();
        // backport: 26.x getCurrentBlockPosOrRailBelow() == NeoForge 1.21.1 getCurrentRailPosition()
        return level.getBlockState(self.getCurrentRailPosition()).getBlock()
                        instanceof IMinecartRail rail
                ? rail
                : null;
    }
}
