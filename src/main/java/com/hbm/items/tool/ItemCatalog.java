// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.ModItems;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import net.minecraft.world.level.Level;
import com.hbm.backport.compat.ItemCompat;

public class ItemCatalog extends ItemCompat {

    public static Consumer<Player> OPEN_SCREEN = player -> {};

    public ItemCatalog(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) OPEN_SCREEN.accept(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> lines,
            TooltipFlag flag) {
        if (stack.is(ModItems.BOBMAZON_HIDDEN.get())) {
            lines.accept(Component.translatable("desc.item.catalog.forAGuideOn"));
            lines.accept(Component.translatable("desc.item.catalog.noTricksThisTime"));
        }
    }
}
