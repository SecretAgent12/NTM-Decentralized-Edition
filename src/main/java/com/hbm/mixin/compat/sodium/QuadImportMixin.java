// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.backport.client.blockmodel.VanillaQuad;
import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.sugar.Local;
// The space before the simple name keeps this import on the 1.21.1 vanilla class (the backport
// redirects the plain name to the 26.x-shaped shim).
import net.minecraft.client.renderer.block.model .BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Copies the HBM light origin of a quad into the tag of Sodium's editor quad when Sodium imports
 * a baked quad, so the light pipelines (FlatLightMixin, SmoothLightMixin) can see it.
 *
 * backport: 26.x injected at RETURN of Sodium 0.9's MutableQuadViewImpl.fromBakedQuad and read
 * the origin from the 26.x BakedQuad.MaterialInfo. Sodium 0.8.x (1.21.1) imports a quad with
 * fromVanilla(BakedQuad, RenderMaterial, Direction), which sets the tag with {@code
 * this.tag(0)}; that argument is replaced instead (no Sodium type in the handler). On 1.21.1 the
 * origin travels on {@link VanillaQuad}; every other quad keeps tag 0 (QuadLighting.VANILLA).
 * Target checked against Sodium 0.8.13 for 1.21.1 (0.8.12 identical).
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl", remap = false)
public class QuadImportMixin {

    @ModifyArg(
            method =
                    "fromVanilla(Lnet/minecraft/client/renderer/block/model/BakedQuad;Lnet/fabricmc/fabric/api/renderer/v1/material/RenderMaterial;Lnet/minecraft/core/Direction;)Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;tag(I)Lnet/caffeinemc/mods/sodium/client/render/frapi/mesh/MutableQuadViewImpl;"),
            index = 0)
    private int hbm$copyOrigin(int tag, @Local(argsOnly = true) BakedQuad quad) {
        return quad instanceof VanillaQuad own && own.lightOrigin() != QuadLighting.VANILLA
                ? own.lightOrigin()
                : tag;
    }
}
