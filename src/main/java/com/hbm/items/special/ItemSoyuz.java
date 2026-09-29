// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.compat.ItemCompat;

public class ItemSoyuz extends ItemCompat {

    public final int skin;
    private final String skinKey;

    public ItemSoyuz(Properties properties, int skin, String skinKey) {
        super(properties);
        this.skin = skin;
        this.skinKey = skinKey;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        ChatFormatting color =
                switch (skin) {
                    case 0 -> ChatFormatting.GOLD;
                    case 1 -> ChatFormatting.BLUE;
                    case 2 -> ChatFormatting.GREEN;
                    default -> throw new IllegalStateException("Unknown Soyuz skin " + skin);
                };
        adder.accept(
                Component.translatable("desc.item.soyuz.skin")
                        .append(" ")
                        .append(Component.translatable(skinKey).withStyle(color)));
    }
}
