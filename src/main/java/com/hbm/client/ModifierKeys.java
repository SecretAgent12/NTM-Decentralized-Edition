// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

public final class ModifierKeys {
    private ModifierKeys() {}

    public static boolean detailsHeld() {
        return (FMLLoader.getDist() == Dist.CLIENT) /* backport: static in 1.21.1 */ && Keybinds.DETAILS.isDown();
    }
}
