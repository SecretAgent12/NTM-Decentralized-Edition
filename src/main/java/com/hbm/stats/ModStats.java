// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.stats;

import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import net.minecraft.resources.ResourceLocation;

public final class ModStats {

    public static RegistryHandle<ResourceLocation> LEGENDARY;

    public static RegistryHandle<ResourceLocation> MINES;

    public static RegistryHandle<ResourceLocation> BULLETS;

    private ModStats() {}

    public static void register(IRegistrar registrar) {

        LEGENDARY = registrar.registerCustomStat("legendary");
        MINES = registrar.registerCustomStat("mines");
        BULLETS = registrar.registerCustomStat("bullets");
    }
}
