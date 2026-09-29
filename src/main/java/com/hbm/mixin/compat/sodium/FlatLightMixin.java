// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.backport.client.sodium.SodiumDucks;
import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium's flat light pipeline for HBM quads (the counterpart of backport
 * BlockmodelQuadLightMixin for vanilla's ModelBlockRenderer):
 *
 * <ul>
 *   <li>OWN_BLOCK quads (OBJ meshes) take the light of their own block position;
 *   <li>cell quads never count their block as a full cube for the parallel-face test.
 * </ul>
 *
 * backport: the 26.x mixin (Sodium 0.9) cancelled calculate at HEAD for OWN_BLOCK quads and
 * filled the output itself: lightmap = getEmissiveLightmap(lightCache.get(pos)), brightness =
 * normal-vector shade when enhanced and not aligned, else the face shade. Sodium 0.8.x has the
 * same branches (cull face / aligned -> face shade, else normal shade when enhanced), so the port
 * keeps Sodium's own brightness, which is what 26.x computed, and only replaces the lightmap it
 * writes ({@code Arrays.fill(out.lm, lightmap)}). The own light is
 * {@link LevelRenderer#getLightColor(net.minecraft.world.level.BlockAndTintGetter, BlockPos)} on
 * the pipeline's level, which Sodium documents as equal to getEmissiveLightmap(get(pos)). No
 * Sodium type appears here: the Sodium-typed arguments are @Coerce'd to Object and read through
 * {@link SodiumDucks}. Targets checked against Sodium 0.8.13 for 1.21.1 (0.8.12 identical):
 * FlatLightPipeline(LightDataAccess), calculate(ModelQuadView, BlockPos, QuadLightData,
 * Direction, Direction, boolean, boolean) with one LightDataAccess.unpackFC(I)Z and one
 * Arrays.fill([II)V. The pipeline instances belong to one block renderer (one thread).
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.flat.FlatLightPipeline", remap = false)
public abstract class FlatLightMixin {
    @Unique private Object hbm$lightCache;
    @Unique private boolean hbm$cell;
    @Unique private int hbm$ownLight = -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hbm$keepCache(@Coerce Object lightCache, CallbackInfo ci) {
        hbm$lightCache = lightCache;
    }

    @Inject(method = "calculate", at = @At("HEAD"))
    private void hbm$ownLight(
            @Coerce Object quad,
            BlockPos pos,
            @Coerce Object out,
            Direction cullFace,
            Direction lightFace,
            boolean shade,
            boolean enhanced,
            CallbackInfo ci) {
        int origin = quad instanceof SodiumDucks.QuadTag view ? view.tag() : QuadLighting.VANILLA;
        hbm$cell = QuadLighting.isCell(origin);
        hbm$ownLight =
                origin == QuadLighting.OWN_BLOCK
                                && hbm$lightCache instanceof SodiumDucks.LightCache cache
                        ? LevelRenderer.getLightColor(cache.getLevel(), pos)
                        : -1;
    }

    @ModifyArg(
            method = "calculate",
            at = @At(value = "INVOKE", target = "Ljava/util/Arrays;fill([II)V"),
            index = 1)
    private int hbm$ownLightmap(int lightmap) {
        return hbm$ownLight >= 0 ? hbm$ownLight : lightmap;
    }

    @ModifyExpressionValue(
            method = "calculate",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/caffeinemc/mods/sodium/client/model/light/data/LightDataAccess;unpackFC(I)Z"))
    private boolean hbm$cellFaces(boolean fullBlock) {
        return fullBlock && !hbm$cell;
    }
}
