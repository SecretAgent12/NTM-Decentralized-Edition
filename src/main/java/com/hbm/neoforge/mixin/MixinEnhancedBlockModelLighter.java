// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin;

import com.hbm.backport.client.blockmodel.VanillaQuad;
import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Arrays;
import net.minecraft.client.renderer.LevelRenderer;
// space before the simple name: keeps this import on the vanilla 1.21.1 BakedQuad (see
// BlockmodelQuadLightMixin)
import net.minecraft.client.renderer.block.model .BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.lighting.FlatQuadLighter;
import net.neoforged.neoforge.client.model.lighting.QuadLighter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 26.x NeoForge's EnhancedBlockModelLighter is, on NeoForge 21.1, the experimental
 * light pipeline's {@link QuadLighter} (LightPipelineAwareModelBlockRenderer; flat and smooth
 * lighters). The same two HBM light origins as on the vanilla lighter (BlockmodelQuadLightMixin):
 *
 * <ul>
 *   <li>OWN_BLOCK quads take the light of their own block position, flat or smooth (26.x
 *       prepareQuadFlat / prepareQuadAmbientOcclusion), brightness kept;
 *   <li>cell quads never count the block as a full collision cube (26.x calculateIrregular): for
 *       the flat lighter its light lookup is redone as FlatQuadLighter.calculateLightmap with
 *       isFullCube false. The 1.21.1 smooth lighter has no full-cube test.
 * </ul>
 */
@Mixin(value = QuadLighter.class, remap = false)
public abstract class MixinEnhancedBlockModelLighter {
    @Unique private static final float HBM_MAX_POSITION = 1f - 1e-2f;
    @Unique private static final byte HBM_MAX_NORMAL = 127;

    @Shadow private BlockAndTintGetter level;
    @Shadow private BlockPos pos;
    @Shadow private BlockState state;
    @Shadow @Final private int[] lightmap;

    @Unique private int hbm$origin = QuadLighting.VANILLA;

    @Inject(
            method = "computeLightingForQuad(Lnet/minecraft/client/renderer/block/model/BakedQuad;)V",
            at = @At("HEAD"))
    private void hbm$quadOrigin(BakedQuad quad, CallbackInfo ci) {
        hbm$origin = quad instanceof VanillaQuad own ? own.lightOrigin() : QuadLighting.VANILLA;
    }

    @WrapOperation(
            method = "computeLightingForQuad([IZ)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/client/model/lighting/QuadLighter;calculateLightmap([F[B)I"))
    private int hbm$cellFaces(
            QuadLighter self, float[] position, byte[] normal, Operation<Integer> original) {
        if (!(self instanceof FlatQuadLighter) || !QuadLighting.isCell(hbm$origin))
            return original.call(self, position, normal);
        Direction side = null;
        if (position[1] < -HBM_MAX_POSITION && normal[1] <= -HBM_MAX_NORMAL) side = Direction.DOWN;
        else if (position[1] > HBM_MAX_POSITION && normal[1] >= HBM_MAX_NORMAL) side = Direction.UP;
        else if (position[2] < -HBM_MAX_POSITION && normal[2] <= -HBM_MAX_NORMAL)
            side = Direction.NORTH;
        else if (position[2] > HBM_MAX_POSITION && normal[2] >= HBM_MAX_NORMAL)
            side = Direction.SOUTH;
        else if (position[0] < -HBM_MAX_POSITION && normal[0] <= -HBM_MAX_NORMAL)
            side = Direction.WEST;
        else if (position[0] > HBM_MAX_POSITION && normal[0] >= HBM_MAX_NORMAL)
            side = Direction.EAST;
        return LevelRenderer.getLightColor(level, state, side == null ? pos : pos.relative(side));
    }

    @Inject(
            method = "computeLightingForQuad(Lnet/minecraft/client/renderer/block/model/BakedQuad;)V",
            at = @At("RETURN"))
    private void hbm$ownBlockLight(BakedQuad quad, CallbackInfo ci) {
        int origin = hbm$origin;
        hbm$origin = QuadLighting.VANILLA;
        if (origin == QuadLighting.OWN_BLOCK)
            Arrays.fill(lightmap, LevelRenderer.getLightColor(level, state, pos));
    }
}
