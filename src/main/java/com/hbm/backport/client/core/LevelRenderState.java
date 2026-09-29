// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.state.level.LevelRenderState}: the parts the tree reads
 * (camera and hovered-block outline). 1.21.1 hooks (RenderLevelStageEvent, RenderHighlightEvent)
 * build one with {@link #of}.
 */
public class LevelRenderState {
    public CameraRenderState cameraRenderState = new CameraRenderState();
    public @Nullable BlockOutlineRenderState blockOutlineRenderState;
    public long gameTime;

    public static LevelRenderState of(Camera camera, @Nullable BlockOutlineRenderState outline) {
        LevelRenderState state = new LevelRenderState();
        state.cameraRenderState = CameraRenderState.of(camera);
        state.blockOutlineRenderState = outline;
        Minecraft mc = Minecraft.getInstance();
        state.gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        return state;
    }

    public static LevelRenderState current() {
        return of(Minecraft.getInstance().gameRenderer.getMainCamera(), null);
    }
}
