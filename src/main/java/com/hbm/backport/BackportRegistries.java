// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.neoforged.bus.api.IEventBus;

/**
 * Registry entries the backport itself adds (shim data components, 1.21.1 armor materials, ...).
 * Called first thing in the mod constructor, before any of the mod's own registration.
 */
public final class BackportRegistries {

    private BackportRegistries() {}

    public static void register(IEventBus modBus) {
        com.hbm.backport.item.armor.ArmorRegistry.register(modBus);
        com.hbm.backport.item.food.ConsumableRegistry.register(modBus);
        com.hbm.backport.item.tool.ToolRegistry.register(modBus);
    }
}
