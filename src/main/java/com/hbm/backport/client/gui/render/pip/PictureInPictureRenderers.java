// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.render.pip;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.gui.state.pip.PictureInPictureRenderState;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Registered picture-in-picture renderers by state class (26.x keeps them in its GuiRenderer). */
public final class PictureInPictureRenderers {

    private static final Map<Class<?>, Supplier<? extends PictureInPictureRenderer<?>>> FACTORIES = new HashMap<>();
    private static final Map<Class<?>, PictureInPictureRenderer<?>> RENDERERS = new HashMap<>();

    private PictureInPictureRenderers() {}

    public static synchronized <T extends PictureInPictureRenderState> void register(
            Class<T> stateClass, Supplier<? extends PictureInPictureRenderer<T>> factory) {
        FACTORIES.put(stateClass, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T extends PictureInPictureRenderState> void render(GuiGraphicsExtractor graphics, T state) {
        PictureInPictureRenderer<T> renderer = (PictureInPictureRenderer<T>) RENDERERS.computeIfAbsent(
                state.getClass(), c -> {
                    Supplier<? extends PictureInPictureRenderer<?>> f = FACTORIES.get(c);
                    return f == null ? null : f.get();
                });
        if (renderer != null) renderer.render(graphics, state);
    }
}
