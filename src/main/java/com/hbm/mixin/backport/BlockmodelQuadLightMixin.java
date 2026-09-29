// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.client.blockmodel.VanillaQuad;
import com.hbm.client.model.QuadLighting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.BitSet;
import net.minecraft.client.renderer.LevelRenderer;
// The two 1.21.1 classes this mixin works on share their names with 26.x classes the backport
// redirects to shims (the backport rewrites imports and qualified names of
// the vanilla ModelBlockRenderer and BakedQuad names). The space
// before the simple name keeps these two imports on the vanilla classes.
import net.minecraft.client.renderer.block .ModelBlockRenderer;
import net.minecraft.client.renderer.block.model .BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The 26.x MixinBlockModelLighter on 1.21.1's ModelBlockRenderer (26.x split the lighting into
 * BlockModelLighter#prepareQuadFlat / prepareQuadAmbientOcclusion / prepareQuadShape):
 *
 * <ul>
 *   <li>quads whose HBM light origin is OWN_BLOCK (OBJ meshes) take the light of their own block
 *       position, flat or with AO (AO brightness kept);
 *   <li>quads carrying a cell light origin never count the block as a full collision cube when
 *       the face-on-boundary flag is computed.
 * </ul>
 *
 * The origin travels on {@link VanillaQuad}, the 1.21.1 form of HBM's 26.x quads. CLIENT mixin.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class BlockmodelQuadLightMixin {

    @Unique
    private static final ThreadLocal<boolean[]> hbm$cellQuad = ThreadLocal.withInitial(() -> new boolean[1]);

    @Unique
    private static int hbm$origin(BakedQuad quad) {
        return quad instanceof VanillaQuad own ? own.lightOrigin() : QuadLighting.VANILLA;
    }

    @WrapOperation(
            method = {"renderModelFaceAO", "renderModelFaceFlat"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIIII)V"))
    private void hbm$ownBlockLight(
            ModelBlockRenderer self,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BakedQuad quad,
            float b0,
            float b1,
            float b2,
            float b3,
            int l0,
            int l1,
            int l2,
            int l3,
            int overlay,
            Operation<Void> original) {
        if (hbm$origin(quad) == QuadLighting.OWN_BLOCK) {
            int own = LevelRenderer.getLightColor(level, state, pos);
            l0 = l1 = l2 = l3 = own;
        }
        original.call(self, level, state, pos, consumer, pose, quad, b0, b1, b2, b3, l0, l1, l2, l3, overlay);
    }

    @WrapOperation(
            method = {"renderModelFaceAO", "renderModelFaceFlat"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;calculateShape(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;[ILnet/minecraft/core/Direction;[FLjava/util/BitSet;)V"))
    private void hbm$markCellQuad(
            ModelBlockRenderer self,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            int[] vertices,
            Direction direction,
            float[] shape,
            BitSet flags,
            Operation<Void> original,
            @Local BakedQuad quad) {
        boolean[] cell = hbm$cellQuad.get();
        cell[0] = QuadLighting.isCell(hbm$origin(quad));
        try {
            original.call(self, level, state, pos, vertices, direction, shape, flags);
        } finally {
            cell[0] = false;
        }
    }

    @ModifyExpressionValue(
            method = "calculateShape",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hbm$cellFaces(boolean fullBlock) {
        return fullBlock && !hbm$cellQuad.get()[0];
    }
}
