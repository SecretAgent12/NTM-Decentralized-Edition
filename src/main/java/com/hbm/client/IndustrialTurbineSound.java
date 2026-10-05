// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.backport.SubLevelSpace;
import com.hbm.tileentity.machine.BlockEntityMachineIndustrialTurbine;
import net.minecraft.client.Minecraft;

public final class IndustrialTurbineSound {

    private IndustrialTurbineSound() {}

    // backport-fix: BF-061 — distance to where the machine is drawn (a contraption is in a far plot)
    public static void tick(BlockEntityMachineIndustrialTurbine be) {
        Minecraft mc = Minecraft.getInstance();
        double x = be.getBlockPos().getX() + 0.5,
                y = be.getBlockPos().getY() + 0.5,
                z = be.getBlockPos().getZ() + 0.5;
        boolean near =
                mc.player != null && mc.player.distanceToSqr(SubLevelSpace.toWorld(mc.player.level(), x, y, z)) <= 35 * 35;

        be.audioLoop(be.spin > 0 && near, be.audioVolume(), be.audioPitch());
    }
}
