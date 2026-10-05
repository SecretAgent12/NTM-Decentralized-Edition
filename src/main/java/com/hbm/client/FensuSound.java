// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.backport.SubLevelSpace;
import com.hbm.tileentity.machine.storage.BlockEntityBatteryREDD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class FensuSound {

    private FensuSound() {}

    // backport-fix: BF-061 — distance to where the machine is drawn (a contraption is in a far plot)
    public static void tick(BlockEntityBatteryREDD be) {
        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                be.getSpeed() > 0F
                        && me != null
                        && me.getEyePosition()
                                        .distanceToSqr(
                                                SubLevelSpace.toWorld(me.level(), pos.getX() + 0.5,
                                                pos.getY() + 5.5,
                                                pos.getZ() + 0.5))
                                < 30 * 30,
                1.5F,
                be.audioPitch());
    }
}
