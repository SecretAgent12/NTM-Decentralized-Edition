// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import com.hbm.backport.compat.ItemCompat;

public class ItemDigammaDiagnostic extends ItemCompat {

    public ItemDigammaDiagnostic(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            ContaminationUtil.printDiagnosticData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
