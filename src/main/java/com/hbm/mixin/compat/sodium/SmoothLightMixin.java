// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.backport.client.sodium.SodiumDucks;
import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Arrays;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium's smooth (AO) light pipeline for HBM quads: OWN_BLOCK quads keep their AO brightness but
 * take the light of their own block position; cell quads never count their block as a full cube
 * for the parallel-face test.
 *
 * backport: same behaviour as the 26.x mixin (Sodium 0.9). No Sodium type appears here: the
 * Sodium-typed arguments are @Coerce'd to Object and read through {@link SodiumDucks}; the cell
 * flag is taken at HEAD (26.x read the quad with @Local in the unpackFC handler). The own light
 * is {@link LevelRenderer#getLightColor(net.minecraft.world.level.BlockAndTintGetter, BlockPos)}
 * on the pipeline's level, which Sodium documents as equal to the 26.x
 * getEmissiveLightmap(lightCache.get(pos)). Targets checked against Sodium 0.8.13 for 1.21.1
 * (0.8.12 identical): SmoothLightPipeline(LightDataAccess), calculate(ModelQuadView, BlockPos,
 * QuadLightData, Direction, Direction, boolean, boolean) with one LightDataAccess.unpackFC(I)Z.
 * The pipeline instances belong to one block renderer (one thread).
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline", remap = false)
public abstract class SmoothLightMixin {
    @Unique private Object hbm$lightCache;
    @Unique private boolean hbm$cell;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hbm$keepCache(@Coerce Object lightCache, CallbackInfo ci) {
        hbm$lightCache = lightCache;
    }

    @Inject(method = "calculate", at = @At("HEAD"))
    private void hbm$cellFlag(
            @Coerce Object quad,
            BlockPos pos,
            @Coerce Object out,
            Direction cullFace,
            Direction lightFace,
            boolean shade,
            boolean enhanced,
            CallbackInfo ci) {
        hbm$cell = QuadLighting.isCell(hbm$origin(quad));
    }

    @Inject(method = "calculate", at = @At("RETURN"))
    private void hbm$ownLight(
            @Coerce Object quad,
            BlockPos pos,
            @Coerce Object out,
            Direction cullFace,
            Direction lightFace,
            boolean shade,
            boolean enhanced,
            CallbackInfo ci) {
        if (hbm$origin(quad) == QuadLighting.OWN_BLOCK
                && out instanceof SodiumDucks.QuadLightData data
                && hbm$lightCache instanceof SodiumDucks.LightCache cache) {
            Arrays.fill(data.hbm$lightmaps(), LevelRenderer.getLightColor(cache.getLevel(), pos));
        }
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

    @Unique
    private static int hbm$origin(Object quad) {
        return quad instanceof SodiumDucks.QuadTag view ? view.tag() : QuadLighting.VANILLA;
    }
}
