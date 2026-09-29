// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import com.hbm.backport.client.gui.input.CharacterEvent;
import com.hbm.backport.client.gui.input.KeyEvent;
import com.hbm.backport.client.gui.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

/**
 * 26.x {@code Screen} hook shapes on a 1.21.1 Screen (the ItemCompat idea): the 1.21.1 render and
 * input hooks are final and call the 26.x-named/-typed ones, whose defaults run the 1.21.1
 * behaviour. Frame order is 26.x's: {@link #extractBackground}, then {@link #extractRenderState}
 * (widgets), then deferred tooltips.
 */
public abstract class ScreenCompat extends Screen {

    protected ScreenCompat(Component title) {
        super(title);
    }

    // ---- rendering ------------------------------------------------------------------------------

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor g = GuiGraphicsExtractor.of(graphics);
        g.beginDeferredTooltips();
        try {
            // backport: unverified: 26.x draws the background before (not inside) extractRenderState
            extractBackground(g, mouseX, mouseY, partialTick);
            extractRenderState(g, mouseX, mouseY, partialTick);
        } finally {
            g.endDeferredTooltips();
        }
    }

    /** 26.x Screen.extractRenderState: the renderable widgets (1.21.1 Screen.render minus background). */
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : this.renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public final void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(GuiGraphicsExtractor.of(graphics), mouseX, mouseY, partialTick);
    }

    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    // ---- input: 1.21.1 hooks (final) -> 26.x event hooks ------------------------------------------

    private final WidgetBridge.ClickTracker backport$clicks = new WidgetBridge.ClickTracker();

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean dbl = backport$clicks.press(button);
        return mouseClicked(new MouseButtonEvent(mouseX, mouseY, button), dbl);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        return mouseReleased(new MouseButtonEvent(mouseX, mouseY, button));
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return mouseDragged(new MouseButtonEvent(mouseX, mouseY, button), dragX, dragY);
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean keyReleased(KeyEvent event) {
        return super.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean charTyped(char codePoint, int modifiers) {
        return charTyped(new CharacterEvent(codePoint, modifiers));
    }

    public boolean charTyped(CharacterEvent event) {
        boolean any = false;
        for (char c : Character.toChars(event.codepoint())) any |= super.charTyped(c, event.modifiers());
        return any;
    }

    /** 26.x Screen.isInGameUi (no 1.21.1 counterpart; nothing reads it here). */
    public boolean isInGameUi() {
        return false;
    }
}
