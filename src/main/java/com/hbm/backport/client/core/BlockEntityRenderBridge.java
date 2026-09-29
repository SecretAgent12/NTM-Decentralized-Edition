// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Runs a 26.x-shaped block entity renderer inside the 1.21.1 immediate {@code render} call:
 * createRenderState -> extractRenderState -> submit into an {@link ImmediateSubmitNodeCollector}.
 */
public final class BlockEntityRenderBridge {
    private BlockEntityRenderBridge() {}

    // the packed light 1.21.1 passed to render(), read by BlockEntityRenderState.extractBase (render
    // thread only; -1 outside a bridged call)
    private static int pendingLight = -1;

    static int pendingLight() {
        return pendingLight;
    }

    public static <T extends BlockEntity, S extends BlockEntityRenderState> void render(
            BlockEntityRenderer<T, S> renderer,
            T be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        CameraRenderState camera = CameraRenderState.current();
        S state = renderer.createRenderState();
        int saved = pendingLight;
        pendingLight = light;
        try {
            // backport: 1.21.1 applies block-breaking crumbling itself through the MultiBufferSource
            // it hands to the renderer, so no CrumblingOverlay is passed here
            renderer.extractRenderState(be, state, partialTick, camera.pos, null);
        } finally {
            pendingLight = saved;
        }
        renderer.submit(state, poseStack, new ImmediateSubmitNodeCollector(buffers), camera);
    }

    /** Extracts a state outside a 1.21.1 render call (item renderers, previews). */
    public static <T extends BlockEntity, S extends BlockEntityRenderState> S extract(
            BlockEntityRenderer<T, S> renderer, T be, float partialTick, int light) {
        S state = renderer.createRenderState();
        int saved = pendingLight;
        pendingLight = light;
        try {
            renderer.extractRenderState(be, state, partialTick, CameraRenderState.current().pos, null);
        } finally {
            pendingLight = saved;
        }
        return state;
    }
}
