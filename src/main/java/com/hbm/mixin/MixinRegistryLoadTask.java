// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.world.feature.Stratum3DConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * backport: 26.x RegistryLoadTask.validateRegistry (RegistryValidator.validate per loaded
 * registry) is 1.21.1 RegistryDataLoader.load's per-loader lambda, which freezes each loaded
 * registry and records its errors in the shared error map ({@code lambda$load$6(Map, Loader)},
 * both the resource and the network load). The stratum domain check runs after a successful
 * freeze, into the same error map, so a clash fails the load like 26.x.
 */
@Mixin(RegistryDataLoader.class)
public abstract class MixinRegistryLoadTask {
    @WrapOperation(
            method = "lambda$load$6",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/core/Registry;freeze()Lnet/minecraft/core/Registry;"))
    private static Registry<?> hbm$validateStratumDomains(
            Registry<?> registry,
            Operation<Registry<?>> original,
            Map<ResourceKey<?>, Exception> errors) {
        Registry<?> frozen = original.call(registry);
        if (registry.key().equals(Registries.CONFIGURED_FEATURE))
            Stratum3DConfig.validateRegistry(registry, errors);
        return frozen;
    }
}
