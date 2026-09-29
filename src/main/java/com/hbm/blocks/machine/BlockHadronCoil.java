// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.mojang.serialization.MapCodec;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import com.hbm.backport.compat.BlockItemCompat;
import com.hbm.backport.compat.BlockCompat;

public class BlockHadronCoil extends BlockCompat {

    public static final MapCodec<BlockHadronCoil> CODEC = simpleCodec(BlockHadronCoil::new);

    private static final int FACTOR = 10;

    public BlockHadronCoil(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static class CoilItem extends BlockItemCompat {

        public CoilItem(Block block, Properties props) {
            super(block, props);
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                Item.TooltipContext context,
                TooltipDisplay display,
                Consumer<Component> adder,
                TooltipFlag flag) {
            adder.accept(
                    Component.translatable("info.coil")
                            .append(": " + String.format(Locale.US, "%,d", FACTOR)));
        }
    }
}
