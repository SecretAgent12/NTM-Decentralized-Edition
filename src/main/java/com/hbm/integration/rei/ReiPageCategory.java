// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.rei;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.integration.recipeviewer.RecipePage;
import com.hbm.integration.recipeviewer.RecipePanel;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * REI integration (our own, not in ntm-next or 1.7.10): a recipe page as an REI category. The
 * page draws itself as it does in JEI ({@link RecipePage#draw}, at the page origin), with the
 * slots, arrows and text it laid out ({@link ReiPageDisplay.Recorded}) on top and its own tooltip
 * ({@link RecipePage#tooltip}) wherever no slot is.
 */
final class ReiPageCategory<T> implements DisplayCategory<ReiPageDisplay<T>> {

    /** Border between REI's recipe panel and the page. */
    private static final int PAD = 0x4;

    private static final int TEXT_DARK = 0xFFBBBBBB;

    private final RecipePage<T> page;
    private final CategoryIdentifier<ReiPageDisplay<T>> id;
    private final Renderer icon;

    ReiPageCategory(RecipePage<T> page) {
        this.page = page;
        this.id = idOf(page);
        this.icon = EntryStacks.of(page.icon());
    }

    static <T> CategoryIdentifier<ReiPageDisplay<T>> idOf(RecipePage<T> page) {
        return CategoryIdentifier.of(page.id());
    }

    CategoryIdentifier<ReiPageDisplay<T>> id() {
        return id;
    }

    @Override
    public CategoryIdentifier<? extends ReiPageDisplay<T>> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Component getTitle() {
        return page.title();
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayHeight() {
        return page.height() + 2 * PAD;
    }

    @Override
    public int getDisplayWidth(ReiPageDisplay<T> display) {
        return page.width() + 2 * PAD;
    }

    @Override
    public List<Widget> setupDisplay(ReiPageDisplay<T> display, Rectangle bounds) {
        int ox = bounds.x + PAD;
        int oy = bounds.y + PAD;
        T row = display.row();
        ReiPageDisplay.Recorded layout = display.layout();

        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(
                Widgets.createDrawableWidget(
                        (graphics, mouseX, mouseY, delta) -> {
                            graphics.pose().pushPose();
                            graphics.pose().translate(ox, oy, 0);
                            page.draw(row, GuiGraphicsExtractor.of(graphics));
                            graphics.pose().popPose();
                        }));
        for (ReiPageDisplay.Arrow arrow : layout.arrows)
            widgets.add(Widgets.createArrow(new Point(ox + arrow.x(), oy + arrow.y())));
        for (ReiPageDisplay.Text text : layout.texts) widgets.add(text(text, ox, oy));

        List<Rectangle> slotAreas = new ArrayList<>();
        for (ReiPageDisplay.SlotSpec spec : layout.slots) {
            Slot slot = Widgets.createSlot(new Point(ox + spec.x, oy + spec.y)).entries(spec.entries);
            switch (spec.role) {
                case INPUT -> slot.markInput();
                case OUTPUT -> slot.markOutput();
                default -> {}
            }
            if (!spec.background) slot.disableBackground();
            widgets.add(slot);
            slotAreas.add(new Rectangle(ox + spec.x - 1, oy + spec.y - 1, 18, 18));
        }

        Rectangle inner = new Rectangle(ox, oy, page.width(), page.height());
        widgets.add(
                Widgets.createTooltip(
                        point -> {
                            if (!inner.contains(point)) return null;
                            // a slot shows its own stack's tooltip; the page's only fills the rest
                            for (Rectangle area : slotAreas) if (area.contains(point)) return null;
                            List<Component> lines = new ArrayList<>();
                            page.tooltip(row, point.x - ox, point.y - oy, lines::add);
                            return lines.isEmpty() ? null : Tooltip.create(point, lines);
                        }));
        return widgets;
    }

    /** Page text, wrapped to its box and cut at its height, in the panel's text colour. */
    private static Widget text(ReiPageDisplay.Text text, int ox, int oy) {
        return Widgets.createDrawableWidget(
                (graphics, mouseX, mouseY, delta) -> {
                    Font font = Minecraft.getInstance().font;
                    int colour =
                            REIRuntime.getInstance().isDarkThemeEnabled()
                                    ? TEXT_DARK
                                    : RecipePanel.TEXT;
                    int y = oy + text.y();
                    int bottom = y + text.height();
                    for (FormattedCharSequence line : font.split(text.text(), text.width())) {
                        if (y + font.lineHeight > bottom) break;
                        graphics.drawString(font, line, ox + text.x(), y, colour, false);
                        y += font.lineHeight;
                    }
                });
    }
}
