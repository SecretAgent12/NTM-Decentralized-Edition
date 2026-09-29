// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.mojang.serialization.MapCodec;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/**
 * 26.x registers loot number providers by their MapCodec; 1.21.1 by a
 * LootNumberProviderType wrapping it, which the provider returns from getType().
 * One type per codec, shared by the registrar and the provider.
 */
public final class LootTypes {

    private static final Map<MapCodec<?>, LootNumberProviderType> NUMBER_PROVIDERS = new IdentityHashMap<>();

    private LootTypes() {}

    public static synchronized LootNumberProviderType numberProvider(MapCodec<? extends NumberProvider> codec) {
        return NUMBER_PROVIDERS.computeIfAbsent(codec, c -> new LootNumberProviderType(codec));
    }
}
