// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data component types standing in for 26.x {@code DataComponents.CONSUMABLE} / {@code USE_REMAINDER}.
 *
 * <p>The types are built eagerly (plain static finals, so {@code DataComponents.CONSUMABLE} can be rewritten to
 * {@code ConsumableRegistry.CONSUMABLE} and used anywhere, like the vanilla constant) and handed to the registry
 * by the DeferredRegister.
 */
public final class ConsumableRegistry {

    public static final DataComponentType<Consumable> CONSUMABLE =
            DataComponentType.<Consumable>builder().persistent(Consumable.CODEC).cacheEncoding().build();

    public static final DataComponentType<UseRemainder> USE_REMAINDER =
            DataComponentType.<UseRemainder>builder().persistent(UseRemainder.CODEC).build();

    private static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "hbm");

    static {
        TYPES.register("backport_consumable", () -> CONSUMABLE);
        TYPES.register("backport_use_remainder", () -> USE_REMAINDER);
    }

    private ConsumableRegistry() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
