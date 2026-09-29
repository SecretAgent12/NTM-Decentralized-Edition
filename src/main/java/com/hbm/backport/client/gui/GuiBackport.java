// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import com.hbm.backport.client.gui.render.pip.RegisterPictureInPictureRenderersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client registration hooks of the GUI backport; the client entry point calls {@link #register}. */
public final class GuiBackport {

    private GuiBackport() {}

    public static void register(IEventBus modBus) {
        // 1.21.1 NeoForge has no picture-in-picture renderer registry: post the 26.x-shaped event
        // ourselves so the mod's listeners fill com.hbm.backport.client.gui.render.pip's registry
        modBus.addListener(
                FMLClientSetupEvent.class,
                event -> event.enqueueWork(() -> modBus.post(new RegisterPictureInPictureRenderersEvent())));
        // fluid tooltip frames recolour the 1.21.1 tooltip border (see FluidTooltipFrame)
        NeoForge.EVENT_BUS.addListener(com.hbm.client.gui.FluidTooltipFrame::onTooltipColor);
    }
}
