// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.client.render.AssemblyMarkers;
import net.minecraft.client.Minecraft;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.SubmitNodeCollector;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeAssemblyMarkers {
    private NeoForgeAssemblyMarkers() {}

    static void register() {
        // backport: NeoForge 26 extracts into the level render state (ExtractLevelRenderStateEvent)
        // and submits in SubmitCustomGeometryEvent; 1.21.1 runs both back to back in
        // RenderLevelStageEvent (after block entities, the stage vanilla draws the block outline in)
        // and draws straight into the main buffer source (flushed by LevelRenderer afterwards).
        NeoForge.EVENT_BUS.addListener(
                (RenderLevelStageEvent event) -> {
                    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
                    CameraRenderState camera = CameraRenderState.of(event.getCamera());
                    Minecraft mc = Minecraft.getInstance();
                    AssemblyMarkers.submit(
                            AssemblyMarkers.extract(camera.pos),
                            event.getPoseStack(),
                            SubmitNodeCollector.immediate(mc.renderBuffers().bufferSource()),
                            camera,
                            mc.font);
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> AssemblyMarkers.clear());
    }
}
