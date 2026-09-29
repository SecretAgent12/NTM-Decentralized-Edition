// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.render.pip;

import com.hbm.backport.client.gui.state.pip.PictureInPictureRenderState;
import java.util.function.Supplier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * NeoForge 26.x {@code RegisterPictureInPictureRenderersEvent}, posted on the mod bus by {@link
 * com.hbm.backport.client.gui.GuiBackport#register} during client setup.
 */
public class RegisterPictureInPictureRenderersEvent extends Event implements IModBusEvent {

    public <T extends PictureInPictureRenderState> void register(
            Class<T> stateClass, Supplier<? extends PictureInPictureRenderer<T>> factory) {
        PictureInPictureRenderers.register(stateClass, factory);
    }
}
