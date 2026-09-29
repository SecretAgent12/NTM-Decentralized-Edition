// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import com.hbm.backport.compat.ItemCompat;

public final class ItemMatch extends ItemCompat {

    public ItemMatch(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult backport$useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.mayUseItemAt(pos, context.getClickedFace(), stack))
            return InteractionResult.FAIL;

        if (level.isEmptyBlock(pos)) {
            level.playSound(
                    player,
                    pos,
                    SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    level.getRandom().nextFloat() * 0.4F + 0.8F);
            level.setBlock(pos, BaseFireBlock.getState(level, pos), 11);
        }

        if (player instanceof ServerPlayer serverPlayer)
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
