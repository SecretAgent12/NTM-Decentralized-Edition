// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin;

import com.hbm.blocks.fluid.InertFluidType;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Inert (classic finite) fluids are not tracked as fluids the entity is in.
 *
 * <p>backport: 26.x returns no tracker from EntityFluidInteraction.getTrackerFor(FluidType) for
 * them. 1.21.1 has no EntityFluidInteraction; NeoForge's Entity tracks fluid types in
 * updateFluidHeightAndDoFluidPushing (height + pushing, skipping air types) and updateFluidOnEyes
 * (eye fluid type). Reporting the empty fluid type for inert fluids there leaves them untracked.
 */
@Mixin(Entity.class)
abstract class EntityFluidInteractionMixin {

    @ModifyExpressionValue(
            method = {"updateFluidHeightAndDoFluidPushing()V", "updateFluidOnEyes"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/material/FluidState;getFluidType()Lnet/neoforged/neoforge/fluids/FluidType;"))
    private FluidType hbm$untracked(FluidType type) {
        return type instanceof InertFluidType ? NeoForgeMod.EMPTY_TYPE.value() : type;
    }
}
