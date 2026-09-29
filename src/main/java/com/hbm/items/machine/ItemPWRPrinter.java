// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.packet.toclient.PwrPrintPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachinePWRController;
import com.hbm.util.ChunkUtil;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import com.hbm.backport.compat.ItemCompat;

public final class ItemPWRPrinter extends ItemCompat {

    public ItemPWRPrinter(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult backport$useOn(UseOnContext context) {
        if (!(ChunkUtil.blockEntityIfLoaded(context.getLevel(), context.getClickedPos())
                instanceof BlockEntityMachinePWRController)) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            PwrPrintData data = PwrPrintData.capture(player.serverLevel(), context.getClickedPos());
            if (data == null) {
                player.sendSystemMessage(Component.translatable("argument.pos.unloaded"));
                return InteractionResult.FAIL;
            }
            Services.NETWORK.sendTo(new PwrPrintPayload(data), player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.pwrPrinter.use"));
    }
}
