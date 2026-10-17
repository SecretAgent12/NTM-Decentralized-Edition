// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.environment;

import java.util.Arrays;
import net.minecraft.world.level.Level;

/**
 * backport: 26.x also has {@code addLayers(EnvironmentAttributeSystem.Builder, Level)} here and on {@link Provider},
 * fed from a mixin on the 26.x environment attribute system. 1.21.1 has no such system, so only the claim half
 * exists: while a provider claims a level, the core leaves that level's sky alone (no impact dust/fire tint, see
 * ImpactAtmosphere#active); an addon draws its sky through 1.21.1 hooks of its own.
 */
public final class AtmosphereLayers {

    public interface Provider {
        boolean claims(Level level);
    }

    private static Provider[] providers = new Provider[0];

    private AtmosphereLayers() {}

    public static void register(Provider provider) {
        providers = Arrays.copyOf(providers, providers.length + 1);
        providers[providers.length - 1] = provider;
    }

    public static boolean claimed(Level level) {
        for (Provider provider : providers) {
            if (provider.claims(level)) return true;
        }
        return false;
    }
}
