// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.misc;

import net.minecraft.resources.ResourceLocation;

/**
 * 26.x {@code KeyMapping.Category} -> 1.21.1 string key categories. The tree's
 * {@code KeyMapping.Category} type becomes {@code String}, static members
 * ({@code register}, constants) come from here.
 */
public final class KeyCategory {

    public static final String MOVEMENT = "key.categories.movement";
    public static final String MISC = "key.categories.misc";
    public static final String MULTIPLAYER = "key.categories.multiplayer";
    public static final String GAMEPLAY = "key.categories.gameplay";
    public static final String INVENTORY = "key.categories.inventory";
    public static final String CREATIVE = "key.categories.creative";
    public static final String SPECTATOR = "key.categories.spectator";
    public static final String DEBUG = "key.categories.debug";

    private KeyCategory() {}

    /**
     * 26.x Category.register(id): label key {@code key.category.<namespace>.<path>} (the key
     * hbm's lang files already use). 1.21.1 needs no registration: unknown categories sort
     * after the vanilla ones by translated name (NeoForge KeyMapping#compareTo).
     */
    public static String register(ResourceLocation id) {
        return "key.category." + id.getNamespace() + "." + id.getPath();
    }
}
