// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.special.ItemSatellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import net.minecraft.world.level.Level;
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
        Runnable ntm =
                () -> {
                    adder.accept(
                            Component.translatable("satchip.frequency")
                                    .append(": " + getFreq(stack))
                                    .withStyle(ChatFormatting.AQUA));
                    if (description == null) return;
                    for (Component line : TooltipStyle.lines(Component.translatable(description)))
                        adder.accept(line);
                };
        SatelliteType type = SatelliteType.fromStack(stack);
        if (type == null) ntm.run();
        else ItemSatellite.DESCRIBE.get().describe(stack, type, context, flag, adder, ntm);
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SatelliteType type = SatelliteType.fromStack(stack);
        return type == null
                ? super.backport$use(level, player, hand)
                : ItemSatellite.USE.get().use(stack, type, level, player, hand);
    }
}
