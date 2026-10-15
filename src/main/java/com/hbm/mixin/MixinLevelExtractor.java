// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.NuclearTech;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import com.hbm.blocks.IBlockHighlight;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.render.CullableRenderer;
import com.hbm.client.render.MultiblockCrumbling;
import com.hbm.client.render.MultiblockOutline;
import com.hbm.client.render.flywheel.FalloutRainVisuals;
import com.hbm.entity.FarVisible;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 26.x LevelExtractor (render-state extraction) does not exist in 1.21.1; the same data is
 * computed inline while LevelRenderer.renderLevel draws. Each 26.x hook sits at the 1.21.1 point
 * that reads the same value:
 *
 * <ul>
 *   <li>extractBlockOutline's shape -> renderHitOutline's getShape (MixinLevelRenderer builds the
 *       LevelRenderState outline with the same shape);
 *   <li>extractVisibleBlockEntities HEAD -> the "blockentities" profiler section; its per-entity
 *       destructionProgress lookup -> the same lookup in renderLevel's section block entity loop;
 *       the global block entities' crumbling overlay -> the global loop's dispatcher render, which
 *       gets 1.21.1's crumbling buffer wrapper (vanilla gives global block entities none);
 *   <li>extractBlockDestroyAnimation's destructionProgress read -> the destroyProgress loop's
 *       destructionProgress read (getfield ordinal 1): a multiblock dummy's breaking texture is
 *       drawn over its owner instead;
 *   <li>extractVisibleEntities' setViewScale (fallout rain columns) -> the "entities" profiler
 *       section (1.21.1 calls setViewScale in setupRender, which has no DeltaTracker);
 *   <li>isEntityVisible's section check -> the entity loop's isSectionCompiled;
 *   <li>setLevel -> setLevel; extract's section-geometry refresh -> right after renderLevel's light
 *       updates (together with 26.x ClientLevel.update's lightApplied), before section compilation.
 * </ul>
 */
@Mixin(LevelRenderer.class)
public class MixinLevelExtractor {
    @Shadow private @Nullable ClientLevel level;
    @Shadow @Final private RenderBuffers renderBuffers;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    private static final String EXTRACT_RENDER_LEVEL =
            "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V";

    @ModifyExpressionValue(
            method =
                    "renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape hbm$machineOutline(
            VoxelShape original,
            @Local(argsOnly = true) Entity entity,
            @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) BlockState state) {
        if (level == null
                || !MultiblockSurface.isSurface(state) && !(state.getBlock() instanceof IBlockHighlight))
            return original;
        return MultiblockOutline.shape(level, pos, state, CollisionContext.of(entity), original);
    }

    @Inject(
            method = EXTRACT_RENDER_LEVEL,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/lighting/LevelLightEngine;runLightUpdates()I",
                            shift = At.Shift.AFTER))
    private void hbm$includeGeometry(CallbackInfo ci) {
        if (level == null) return;
        level.hbm$sectionGeometryIndex().lightApplied();
        level.hbm$sectionGeometryIndex().emptySections(level);
    }

    @Inject(
            method = EXTRACT_RENDER_LEVEL,
            at =
                    @At(
                            value = "INVOKE_STRING",
                            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                            args = "ldc=entities"))
    private void hbm$falloutRainColumns(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        if (level != null) FalloutRainVisuals.capture(camera, deltaTracker, level);
    }

    @Inject(
            method = EXTRACT_RENDER_LEVEL,
            at =
                    @At(
                            value = "INVOKE_STRING",
                            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                            args = "ldc=blockentities"))
    private void hbm$collectCrumbling(CallbackInfo ci) {
        if (level != null) MultiblockCrumbling.collect(level);
    }

    @SuppressWarnings("unchecked")
    @WrapOperation(
            method = EXTRACT_RENDER_LEVEL,
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;",
                            remap = false))
    private Object hbm$coreProgress(
            Long2ObjectMap<?> progress, long pos, Operation<Object> original) {
        return MultiblockCrumbling.progressAt(
                pos, (SortedSet<BlockDestructionProgress>) original.call(progress, pos));
    }

    // BlockEntityRenderDispatcher.render ordinal 1 = the global block entities
    @WrapOperation(
            method = EXTRACT_RENDER_LEVEL,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
                            ordinal = 1))
    private void hbm$globalProgress(
            BlockEntityRenderDispatcher dispatcher,
            BlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Operation<Void> original,
            @Local(argsOnly = true) Camera camera) {
        ModelFeatureRenderer.CrumblingOverlay overlay =
                level == null ? null : MultiblockCrumbling.globalOverlay(blockEntity, camera, level);
        if (overlay != null && overlay.progress() >= 0) {
            // the section loop's crumbling wrapper (pose = the block entity's translation)
            VertexConsumer decal =
                    new SheetedDecalTextureGenerator(
                            renderBuffers
                                    .crumblingBufferSource()
                                    .getBuffer(ModelBakery.DESTROY_TYPES.get(overlay.progress())),
                            poseStack.last(),
                            1.0F);
            MultiBufferSource base = buffers;
            buffers =
                    type -> {
                        VertexConsumer own = base.getBuffer(type);
                        return type.affectsCrumbling() ? VertexMultiConsumer.create(decal, own) : own;
                    };
        }
        original.call(dispatcher, blockEntity, partialTick, poseStack, buffers);
    }

    @ModifyExpressionValue(
            method = EXTRACT_RENDER_LEVEL,
            at =
                    @At(
                            value = "FIELD",
                            opcode = Opcodes.GETFIELD,
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;destructionProgress:Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;",
                            ordinal = 1))
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> hbm$coreBreaking(
            Long2ObjectMap<SortedSet<BlockDestructionProgress>> mined) {
        return level == null ? mined : MultiblockCrumbling.owners(level, mined);
    }

    @WrapOperation(
            method = EXTRACT_RENDER_LEVEL,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiled(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hbm$unculledEntity(
            LevelRenderer renderer,
            BlockPos pos,
            Operation<Boolean> original,
            @Local Entity entity) {
        if (original.call(renderer, pos) || entity instanceof FarVisible) return true;
        return NuclearTech.MOD_ID.equals(
                        BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace())
                && entityRenderDispatcher.getRenderer(entity) instanceof CullableRenderer cullable
                && !cullable.hbm$affectedByCulling(entity);
    }

    @Inject(method = "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V", at = @At("HEAD"))
    private void hbm$releaseGeometry(@Nullable ClientLevel next, CallbackInfo ci) {
        if (level != null && level != next) level.hbm$sectionGeometryIndex().clear();
    }
}
