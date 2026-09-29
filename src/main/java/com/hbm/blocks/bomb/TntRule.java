// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;

public final class TntRule {

    private TntRule() {}

    public static boolean explodes(Level level) {
        // backport: 1.21.1 has no tntExplodes game rule; TNT always explodes there
        return level instanceof ServerLevel;
    }
}
