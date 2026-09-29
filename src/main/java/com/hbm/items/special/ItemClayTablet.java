// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.hbm.backport.compat.ItemCompat;

public class ItemClayTablet extends ItemCompat {

    public static Consumer<Player> OPEN_SCREEN = player -> {};

    public final int recipeSet;

    public ItemClayTablet(Properties properties, int recipeSet) {
        super(properties);
        this.recipeSet = recipeSet;
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && !stack.has(ModDataComponents.TABLET_SEED.get())) {
            stack.set(ModDataComponents.TABLET_SEED.get(), player.getRandom().nextLong());
        }
        if (level.isClientSide()) OPEN_SCREEN.accept(player);
        return InteractionResult.SUCCESS;
    }
}
