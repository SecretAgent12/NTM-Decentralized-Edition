// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T, S>} (extract state,
 * then submit) as a 1.21.1 {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}:
 * the 1.21.1 {@code render} call runs both phases immediately ({@link BlockEntityRenderBridge}), so
 * such renderers register with 1.21.1's BlockEntityRenderers / RegisterRenderers unchanged.
 */
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState>
        extends net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T> {

    S createRenderState();

    default void extractRenderState(
            T blockEntity,
            S state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
    }

    void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera);

    default boolean shouldRenderOffScreen() {
        return false;
    }

    @Override
    default boolean shouldRenderOffScreen(T blockEntity) {
        return shouldRenderOffScreen();
    }

    @Override
    default void render(
            T blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        BlockEntityRenderBridge.render(this, blockEntity, partialTick, poseStack, buffers, light, overlay);
    }
}
