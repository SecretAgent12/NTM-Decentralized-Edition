// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Backport of 26.x's TooltipDisplay (which components a tooltip may show).
 * 1.21.1 has no TOOLTIP_DISPLAY component: the tree's uses of it are rewritten --
 * reading it yields {@link #DEFAULT}, and the one place that hides a component (UNBREAKABLE on wearless
 * armor) uses 1.21.1's own {@code Unbreakable(showInTooltip = false)} instead. Instances made with
 * {@link #withHidden} still answer {@link #shows} correctly for code that passes them around.
 */
public final class TooltipDisplay {
    public static final TooltipDisplay DEFAULT = new TooltipDisplay(false, Set.of());

    private final boolean hideTooltip;
    private final Set<DataComponentType<?>> hiddenComponents;

    private TooltipDisplay(boolean hideTooltip, Set<DataComponentType<?>> hiddenComponents) {
        this.hideTooltip = hideTooltip;
        this.hiddenComponents = hiddenComponents;
    }

    public TooltipDisplay withHidden(DataComponentType<?> type, boolean hidden) {
        if (hiddenComponents.contains(type) == hidden) return this;
        Set<DataComponentType<?>> set = new LinkedHashSet<>(hiddenComponents);
        if (hidden) set.add(type);
        else set.remove(type);
        return new TooltipDisplay(hideTooltip, Set.copyOf(set));
    }

    public boolean shows(DataComponentType<?> type) {
        return !hideTooltip && !hiddenComponents.contains(type);
    }

    public boolean hideTooltip() {
        return hideTooltip;
    }

    public Set<DataComponentType<?>> hiddenComponents() {
        return hiddenComponents;
    }

    /**
     * 26.x {@code item.appendHoverText(stack, context, display, adder, flag)} on a plain Item receiver
     * (1.21.1 Item only has the List form; ItemCompat subclasses bridge that one back to their 26.x
     * override with {@link #DEFAULT}, the only display 1.21.1 ever has).
     */
    public static void appendHoverText(
            Item item,
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        List<Component> lines = new ArrayList<>();
        item.appendHoverText(stack, context, lines, flag);
        lines.forEach(adder);
    }
}
