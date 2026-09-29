// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.ItemCompat;

public class ItemSatelliteChip extends ItemCompat implements ISatChip {

    private final @Nullable String description;

    public ItemSatelliteChip(Properties properties) {
        this(properties, null);
    }

    public ItemSatelliteChip(Properties properties, @Nullable String description) {
        super(properties);
        this.description = description;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("satchip.frequency")
                        .append(": " + getFreq(stack))
                        .withStyle(ChatFormatting.AQUA));
        if (description == null) return;
        for (String line : I18nUtil.resolveKeyArray(description))
            adder.accept(Component.literal(line));
    }
}
