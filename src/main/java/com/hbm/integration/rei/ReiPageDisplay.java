// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.rei;

import com.hbm.integration.recipeviewer.PageLayout;
import com.hbm.integration.recipeviewer.PageSlot;
import com.hbm.integration.recipeviewer.RecipePage;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import dev.architectury.hooks.fluid.FluidStackHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * REI integration (our own, not in ntm-next or 1.7.10): one row of a recipe page as an REI display.
 *
 * <p>The page lays the row out once, into {@link Recorded}: slots with their role, position and
 * stacks, arrows and text. REI's lookups (recipes / uses) read the input and output slots; the
 * category ({@link ReiPageCategory}) turns the same record into widgets, so a row is laid out once
 * however often it is shown. Catalyst and display slots are drawn but take no part in lookups, as
 * on the JEI pages.
 */
public final class ReiPageDisplay<T> implements Display {

    /** mB shown for a fluid slot that gives no amount (a plain "this fluid"). */
    private static final long BUCKET_MB = 0x3E8;

    private final RecipePage<T> page;
    private final CategoryIdentifier<ReiPageDisplay<T>> category;
    private final T row;
    private final Recorded layout;

    ReiPageDisplay(RecipePage<T> page, CategoryIdentifier<ReiPageDisplay<T>> category, T row) {
        this.page = page;
        this.category = category;
        this.row = row;
        this.layout = new Recorded();
        page.layout(row, layout);
        layout.seal();
    }

    RecipePage<T> page() {
        return page;
    }

    T row() {
        return row;
    }

    Recorded layout() {
        return layout;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return layout.ingredients(Role.INPUT);
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return layout.ingredients(Role.OUTPUT);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(page.rowId(row));
    }

    enum Role {
        INPUT,
        OUTPUT,
        CATALYST,
        DISPLAY
    }

    record Arrow(int x, int y) {}

    record Text(Component text, int x, int y, int width, int height) {}

    /** A slot as the page set it up; its stacks get their tooltips once the layout is done. */
    static final class SlotSpec implements PageSlot {

        final Role role;
        final int x;
        final int y;
        boolean background;
        int pressure;
        final List<EntryStack<?>> entries = new ArrayList<>();
        private final List<PageSlot.Tooltip> tooltips = new ArrayList<>();

        SlotSpec(Role role, int x, int y) {
            this.role = role;
            this.x = x;
            this.y = y;
        }

        @Override
        public PageSlot background() {
            background = true;
            return this;
        }

        @Override
        public PageSlot item(ItemStack stack) {
            if (!stack.isEmpty()) entries.add(EntryStacks.of(stack));
            return this;
        }

        @Override
        public PageSlot items(List<ItemStack> stacks) {
            for (ItemStack stack : stacks) item(stack);
            return this;
        }

        @Override
        public PageSlot fluid(Fluid fluid, long milliBuckets, int pressure) {
            // a fluid without an amount is still shown, as one bucket
            long mB = milliBuckets > 0 ? milliBuckets : BUCKET_MB;
            entries.add(EntryStacks.of(fluid, mB * unitsPerMilliBucket()));
            if (pressure > this.pressure) this.pressure = pressure;
            return this;
        }

        @Override
        public PageSlot fluid(Fluid fluid) {
            return fluid(fluid, BUCKET_MB, 0);
        }

        @Override
        public PageSlot tooltip(PageSlot.Tooltip tooltip) {
            tooltips.add(tooltip);
            return this;
        }

        private void seal() {
            if (pressure <= 0 && tooltips.isEmpty()) return;
            for (EntryStack<?> entry : entries) addTooltip(entry);
        }

        private <V> void addTooltip(EntryStack<V> entry) {
            entry.tooltipProcessor(
                    (stack, tooltip) -> {
                        if (pressure > 0) FluidTraitTooltip.addPressureInfo(pressure, tooltip::add);
                        ItemStack shown = stack.getValue() instanceof ItemStack item ? item : null;
                        for (PageSlot.Tooltip extra : tooltips) extra.append(shown, tooltip::add);
                        return tooltip;
                    });
        }

        EntryIngredient ingredient() {
            return EntryIngredient.of(entries);
        }
    }

    /** What one row's layout produced. */
    static final class Recorded implements PageLayout {

        final List<SlotSpec> slots = new ArrayList<>();
        final List<Arrow> arrows = new ArrayList<>();
        final List<Text> texts = new ArrayList<>();

        private SlotSpec slot(Role role, int x, int y) {
            SlotSpec slot = new SlotSpec(role, x, y);
            slots.add(slot);
            return slot;
        }

        @Override
        public PageSlot input(int x, int y) {
            return slot(Role.INPUT, x, y);
        }

        @Override
        public PageSlot output(int x, int y) {
            return slot(Role.OUTPUT, x, y);
        }

        @Override
        public PageSlot catalyst(int x, int y) {
            return slot(Role.CATALYST, x, y);
        }

        @Override
        public PageSlot display(int x, int y) {
            return slot(Role.DISPLAY, x, y);
        }

        @Override
        public void arrow(int x, int y) {
            arrows.add(new Arrow(x, y));
        }

        @Override
        public void text(Component text, int x, int y, int width, int height) {
            texts.add(new Text(text, x, y, width, height));
        }

        private void seal() {
            for (SlotSpec slot : slots) slot.seal();
        }

        List<EntryIngredient> ingredients(Role role) {
            List<EntryIngredient> out = new ArrayList<>();
            for (SlotSpec slot : slots) {
                if (slot.role == role && !slot.entries.isEmpty()) out.add(slot.ingredient());
            }
            return out;
        }
    }

    /** Architectury fluid units in one mB (1 on NeoForge, where a bucket is 1000). */
    private static long unitsPerMilliBucket() {
        return FluidStackHooks.bucketAmount() / 1000L;
    }
}
