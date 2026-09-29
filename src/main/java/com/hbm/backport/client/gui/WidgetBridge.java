// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import com.hbm.backport.client.gui.input.CharacterEvent;
import com.hbm.backport.client.gui.input.KeyEvent;
import com.hbm.backport.client.gui.input.MouseButtonEvent;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * 26.x input/render calls made on 1.21.1 vanilla widgets and listeners (EditBox, Button, a
 * screen's focused child...): {@code widget.keyPressed(event)} ->
 * {@code WidgetBridge.keyPressed(widget, event)} , and the 1.21.1 ->
 * 26.x event conversion used by {@link ScreenCompat} / {@link ContainerScreenCompat}.
 */
public final class WidgetBridge {

    private WidgetBridge() {}

    // ---- 26.x call on a 1.21.1 listener ---------------------------------------------------------

    public static boolean mouseClicked(GuiEventListener l, MouseButtonEvent event, boolean doubleClick) {
        return l.mouseClicked(event.x(), event.y(), event.button());
    }

    public static boolean mouseReleased(GuiEventListener l, MouseButtonEvent event) {
        return l.mouseReleased(event.x(), event.y(), event.button());
    }

    public static boolean mouseDragged(GuiEventListener l, MouseButtonEvent event, double dragX, double dragY) {
        return l.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    public static boolean keyPressed(GuiEventListener l, KeyEvent event) {
        return l.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public static boolean keyReleased(GuiEventListener l, KeyEvent event) {
        return l.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    public static boolean charTyped(GuiEventListener l, CharacterEvent event) {
        boolean any = false;
        for (char c : Character.toChars(event.codepoint())) any |= l.charTyped(c, event.modifiers());
        return any;
    }

    /** 26.x KeyMapping.matches(KeyEvent). */
    public static boolean matches(net.minecraft.client.KeyMapping key, KeyEvent event) {
        return key.matches(event.key(), event.scancode());
    }

    /** 26.x KeyMapping.matchesMouse(MouseButtonEvent). */
    public static boolean matchesMouse(net.minecraft.client.KeyMapping key, MouseButtonEvent event) {
        return key.matchesMouse(event.button());
    }

    public static void extractRenderState(Renderable r, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        r.render(graphics, mouseX, mouseY, partialTick);
    }

    // ---- 1.21.1 hook -> 26.x event ---------------------------------------------------------------

    /** Tracks the last press so 1.21.1 clicks carry 26.x's doubleClick flag. */
    public static final class ClickTracker {
        private long lastTime;
        private int lastButton = -1;

        /** 26.x MouseHandler: same button again within 250 ms. */
        public boolean press(int button) {
            long now = Util.getMillis();
            boolean dbl = button == lastButton && now - lastTime < 250L;
            lastTime = now;
            lastButton = button;
            return dbl;
        }
    }
}
