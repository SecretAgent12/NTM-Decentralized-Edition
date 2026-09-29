// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.client.render.RebarPlacerPreview;
import net.minecraft.client.Minecraft;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.SubmitNodeCollector;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeRebarPlacerPreview {
    private NeoForgeRebarPlacerPreview() {}

    static void register() {
        // backport: NeoForge 26 ExtractLevelRenderStateEvent + SubmitCustomGeometryEvent -> 1.21.1
        // RenderLevelStageEvent (extract and submit in the same frame, after block entities)
        NeoForge.EVENT_BUS.addListener(
                (RenderLevelStageEvent event) -> {
                    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
                    RebarPlacerPreview.submit(
                            RebarPlacerPreview.extract(),
                            event.getPoseStack(),
                            SubmitNodeCollector.immediate(
                                    Minecraft.getInstance().renderBuffers().bufferSource()),
                            CameraRenderState.of(event.getCamera()));
                });
    }
}
