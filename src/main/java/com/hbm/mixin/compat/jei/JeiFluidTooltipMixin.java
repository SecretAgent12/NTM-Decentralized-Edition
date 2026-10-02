// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.jei;

import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport-fix: BF-031 NTM fluids are NeoForge fluids in ntm-next, so JEI shows them as its own
 * fluid ingredients (the square icons, in the list and in recipes) and only prints the name. In
 * 1.7.10 NEI showed fluid icon items whose tooltip listed the fluid's traits (temperature,
 * flammable, corrosive, ...). This appends the same lines (FluidTraitTooltip, as on
 * ItemFluidIcon and the machine GUIs) to every JEI fluid tooltip; non-NTM fluids get nothing.
 *
 * <p>JEI's NeoForge FluidHelper is internal: targeted by name and require = 0, so a JEI version
 * with another signature just skips it instead of failing to start.
 */
@Pseudo
@Mixin(targets = "mezz.jei.neoforge.platform.FluidHelper", remap = false)
public abstract class JeiFluidTooltipMixin {

    @Inject(method = "getTooltip", at = @At("TAIL"), require = 0)
    private void hbm$fluidTraits(
            List<Component> tooltip, FluidStack ingredient, TooltipFlag flag, CallbackInfo ci) {
        if (ingredient.isEmpty()) return;
        FluidTraitTooltip.addInfo(ingredient.getFluid(), tooltip::add);
    }
}
