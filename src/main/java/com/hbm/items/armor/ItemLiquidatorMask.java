// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class ItemLiquidatorMask extends ModArmorItem implements IGasMask {

    public ItemLiquidatorMask(Properties properties) {
        super(properties, Suit.LIQUIDATOR);
    }

    @Override
    public HazardClass[] filterBlacklist() {
        return new HazardClass[0];
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (ArmorUtil.ejectGasMaskFilter(player, player.getItemInHand(hand)))
            return InteractionResult.SUCCESS;
        return super.backport$use(level, player, hand);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        ArmorUtil.addGasMaskTooltip(stack, context, flag, adder);
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
