// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.backport.SubLevelSpace;
import com.hbm.tileentity.machine.BlockEntityMachineChemicalPlant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class ChemPlantSound {

    private ChemPlantSound() {}

    // backport-fix: BF-061 — distance to where the machine is drawn (a contraption is in a far plot)
    public static void tick(BlockEntityMachineChemicalPlant be) {
        LocalPlayer me = Minecraft.getInstance().player;
        BlockPos pos = be.getBlockPos();

        be.audioLoop(
                be.isProgressing
                        && me != null
                        && me.distanceToSqr(SubLevelSpace.toWorld(me.level(), pos.getX(), pos.getY(), pos.getZ()))
                                < 30 * 30,
                1F);
    }
}
