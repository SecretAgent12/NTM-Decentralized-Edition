// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.client.core.BlockOutlineRenderState;
import com.hbm.backport.client.core.LevelRenderState;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.blocks.IBlockHighlight;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.render.ArmorOverheadRenderer;
import com.hbm.client.render.CargoElevatorOutline;
import com.hbm.client.render.ConveyorPreviewRenderer;
import com.hbm.client.render.GlyphidPathRenderer;
import com.hbm.client.render.MultiblockOutline;
import com.hbm.client.render.flywheel.VisualTextures;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 26.x world hooks mapped onto 1.21.1 LevelRenderer.renderLevel:
 *
 * <ul>
 *   <li>render HEAD -> renderLevel HEAD;
 *   <li>the sky pass extras -> MixinSkyRenderer (renderSky's end);
 *   <li>submitBlockOutline HEAD (armor overhead HUD, conveyor preview, glyphid paths, drill
 *       highlight / cargo elevator outline replacing the block outline) -> right before
 *       renderLevel's outline section (the second read of Minecraft.hitResult), with a
 *       LevelRenderState built from the camera and the hovered block (as 26.x extractBlockOutline
 *       does, incl. the multiblock outline shape) and an immediate collector on the level's buffer
 *       source; a cancel skips the block outline, i.e. NeoForge's RenderHighlightEvent.Block and the
 *       vanilla outline (26.x cancelled NeoForge's custom outline submissions with it too).
 * </ul>
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Shadow @Final private RenderBuffers renderBuffers;
    @Shadow private @Nullable ClientLevel level;

    @Unique private boolean hbm$skipOutline;

    private static final String OUTLINE_RENDER_LEVEL =
            "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V";

    @Inject(method = OUTLINE_RENDER_LEVEL, at = @At("HEAD"))
    private void hbm$animateVisualBodies(CallbackInfo ci) {
        VisualTextures.activateSprites();
    }

    @Inject(
            method = OUTLINE_RENDER_LEVEL,
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;",
                            opcode = Opcodes.GETFIELD,
                            ordinal = 1))
    private void hbm$worldOverlays(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f viewRotation,
            Matrix4f projection,
            CallbackInfo ci) {
        hbm$skipOutline = false;
        ClientLevel level = this.level;
        if (level == null) return;
        Minecraft mc = Minecraft.getInstance();
        LevelRenderState levelRenderState =
                LevelRenderState.of(camera, hbm$outline(level, mc.hitResult, renderBlockOutline, camera));
        PoseStack poseStack = new PoseStack();
        SubmitNodeCollector collector = SubmitNodeCollector.immediate(renderBuffers.bufferSource());

        ArmorOverheadRenderer.submit(poseStack, collector, levelRenderState);
        ConveyorPreviewRenderer.submit(poseStack, collector, levelRenderState);
        GlyphidPathRenderer.submit(poseStack, collector, levelRenderState);

        LocalPlayer player = mc.player;
        if (player == null) return;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemGunDrill)) {
            if (CargoElevatorOutline.submit(poseStack, collector, levelRenderState))
                hbm$skipOutline = true;
            return;
        }
        if (levelRenderState.blockOutlineRenderState != null) {
            XFactoryDrill.submitBlockHighlight(
                    poseStack, collector, levelRenderState.cameraRenderState.pos, player, held);
        }
        hbm$skipOutline = true;
    }

    /** 26.x LevelExtractor.extractBlockOutline (with MixinLevelExtractor's multiblock shape). */
    @Unique
    private static @Nullable BlockOutlineRenderState hbm$outline(
            ClientLevel level, @Nullable HitResult hit, boolean renderBlockOutline, Camera camera) {
        if (!renderBlockOutline || !(hit instanceof BlockHitResult blockHit)) return null;
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !level.getWorldBorder().isWithinBounds(pos)) return null;
        CollisionContext context = CollisionContext.of(camera.getEntity());
        VoxelShape shape = state.getShape(level, pos, context);
        if (MultiblockSurface.isSurface(state) || state.getBlock() instanceof IBlockHighlight)
            shape = MultiblockOutline.shape(level, pos, state, context, shape);
        return BlockOutlineRenderState.of(
                pos, ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.translucent(), shape);
    }

    // ClientHooks.onDrawHighlight ordinal 0 = the block-hit branch; true = handled, skip vanilla
    @WrapOperation(
            method = OUTLINE_RENDER_LEVEL,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/client/ClientHooks;onDrawHighlight(Lnet/minecraft/client/renderer/LevelRenderer;Lnet/minecraft/client/Camera;Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/client/DeltaTracker;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)Z",
                            ordinal = 0,
                            remap = false))
    private boolean hbm$drillHighlight(
            LevelRenderer renderer,
            Camera camera,
            HitResult hit,
            DeltaTracker deltaTracker,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Operation<Boolean> original) {
        if (hbm$skipOutline) return true;
        return original.call(renderer, camera, hit, deltaTracker, poseStack, buffers);
    }
}
