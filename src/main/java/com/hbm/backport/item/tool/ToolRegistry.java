// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.tool;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

/**
 * Data component types standing in for the 26.x tool/weapon components that 1.21.1 lacks.
 * Their behaviour is applied natively by {@code com.hbm.mixin.backport.MixinItemToolShims}
 * and {@code MixinItemStackWeapon}; those only act when a component is present.
 */
public final class ToolRegistry {

    private ToolRegistry() {}

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "hbm");

    /** 26.x DataComponents.WEAPON. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Weapon>> WEAPON =
            COMPONENTS.registerComponentType(
                    "backport_weapon",
                    b -> b.persistent(Weapon.CODEC).networkSynchronized(Weapon.STREAM_CODEC));

    /** 26.x DataComponents.ENCHANTABLE. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Enchantable>>
            ENCHANTABLE =
                    COMPONENTS.registerComponentType(
                            "backport_enchantable",
                            b ->
                                    b.persistent(Enchantable.CODEC)
                                            .networkSynchronized(Enchantable.STREAM_CODEC));

    /** 26.x DataComponents.REPAIRABLE. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Repairable>>
            REPAIRABLE =
                    COMPONENTS.registerComponentType(
                            "backport_repairable",
                            b ->
                                    b.persistent(Repairable.CODEC)
                                            .networkSynchronized(Repairable.STREAM_CODEC));

    /**
     * Marks a 26.x {@code Tool} whose {@code canDestroyBlocksInCreative} was false (swords):
     * 1.21.1's Tool record has no such field, so the flag rides along as its own component and
     * is honoured in Item.canAttackBlock (what 1.21.1 SwordItem does natively).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>>
            NO_CREATIVE_DESTROY =
                    COMPONENTS.registerComponentType(
                            "backport_no_creative_destroy",
                            b ->
                                    b.persistent(Unit.CODEC)
                                            .networkSynchronized(
                                                    net.minecraft.network.codec.StreamCodec.unit(
                                                            Unit.INSTANCE)));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    /** Component value, or null when absent or when the type is not registered yet. */
    public static <T> @Nullable T get(
            DataComponentHolder holder,
            DeferredHolder<DataComponentType<?>, DataComponentType<T>> type) {
        return type.isBound() ? holder.get(type.get()) : null;
    }

    /** Component value from a plain map (an item's default components). */
    public static <T> @Nullable T get(
            net.minecraft.core.component.DataComponentMap map,
            DeferredHolder<DataComponentType<?>, DataComponentType<T>> type) {
        return type.isBound() ? map.get(type.get()) : null;
    }
}
