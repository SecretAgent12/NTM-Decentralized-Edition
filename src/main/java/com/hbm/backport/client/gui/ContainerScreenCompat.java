// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import com.hbm.backport.client.gui.input.CharacterEvent;
import com.hbm.backport.client.gui.input.KeyEvent;
import com.hbm.backport.client.gui.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * 26.x {@code AbstractContainerScreen} hook shapes on the 1.21.1 one. 1.21.1 hooks are final and
 * call the 26.x ones: render -> extractBackground + extractRenderState (+ extractTooltip) with
 * deferred tooltips; renderBg -> nothing (26.x container screens draw their texture in
 * extractBackground, which replaces 1.21.1 renderBackground+renderBg); extractLabels before the
 * slots and extractSlots after them (both in the leftPos/topPos-translated pose); renderTooltip ->
 * extractTooltip; input -> the 26.x event hooks.
 *
 * <p>Layering: 26.x draws the GUI in submission order; 1.21.1 depth-tests item icons (z 100-250)
 * against later 2D draws at z 0. The depth buffer is reset after the slot items and after each
 * 26.x {@code graphics.item(...)} so later draws land on top, as in 26.x.
 */
public abstract class ContainerScreenCompat<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    private boolean backport$inVanillaRender;
    private boolean backport$labelsDone;
    private int backport$mouseX;
    private int backport$mouseY;

    public ContainerScreenCompat(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** 26.x constructor with the image size. */
    public ContainerScreenCompat(T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(menu, inventory, title);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.inventoryLabelY = imageHeight - 94;
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

    /**
     * 26.x AbstractContainerScreen.extractRenderState: widgets, slots, labels, carried item (the
     * 1.21.1 render, whose background call is skipped here), then the hovered-slot tooltip.
     */
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        backport$inVanillaRender = true;
        backport$labelsDone = false;
        backport$mouseX = mouseX;
        backport$mouseY = mouseY;
        try {
            super.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            backport$inVanillaRender = false;
        }
        // what follows (subclass overlays, tooltips) is drawn over the slot items, as in 26.x
        graphics.resetDepth();
        extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public final void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (backport$inVanillaRender) return; // drawn by render() before extractRenderState
        extractBackground(GuiGraphicsExtractor.of(graphics), mouseX, mouseY, partialTick);
    }

    /** 26.x: the screen background (1.21.1 Screen.renderBackground) plus, in subclasses, the GUI texture. */
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 26.x has no renderBg: container textures are drawn in extractBackground
    }

    /**
     * 26.x draws the labels before the slots (several tree screens draw their GUI texture in
     * extractLabels); 1.21.1 after. The labels are drawn at the first slot of the frame instead
     * (the pose is already translated to leftPos/topPos there), or in renderLabels without slots.
     *
     * <p>backport-fix: BF-072 — only a slot of this menu counts. TrashSlot draws its own trash slot
     * through renderSlot from the background event, before the pose is translated; taking that
     * call drew the labels (and the GUI textures some screens draw with them) at the screen corner
     * and blocked the real draw for the frame.
     */
    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (backport$inVanillaRender && !backport$labelsDone && this.menu.slots.contains(slot)) {
            backport$labelsDone = true;
            // 1.21.1 resets hoveredSlot before this loop and sets it while drawing slots; labels
            // that ask for it (tooltips) get this frame's hovered slot
            this.hoveredSlot = backport$slotAt(backport$mouseX, backport$mouseY);
            extractLabels(GuiGraphicsExtractor.of(graphics), backport$mouseX, backport$mouseY);
        }
        super.renderSlot(graphics, slot);
    }

    private Slot backport$slotAt(int mouseX, int mouseY) {
        for (Slot s : this.menu.slots) {
            if (s.isActive() && isHovering(s.x, s.y, 16, 16, mouseX, mouseY)) return s;
        }
        return null;
    }

    @Override
    protected final void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiGraphicsExtractor g = GuiGraphicsExtractor.of(graphics);
        if (!backport$labelsDone) {
            backport$labelsDone = true;
            extractLabels(g, mouseX, mouseY);
        }
        // 1.21.1 already drew the slots; extractSlots overrides add to them, above the items
        g.resetDepth();
        extractSlots(g, mouseX, mouseY);
    }

    /** 26.x extractSlots: the slots (already drawn by 1.21.1's render) and whatever subclasses add. */
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    /** 26.x extractLabels (1.21.1 renderLabels: title and inventory label). */
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected final void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        extractTooltip(GuiGraphicsExtractor.of(graphics), mouseX, mouseY);
    }

    /** 26.x extractTooltip: the hovered slot's item tooltip. */
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
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
