// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.hazard;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointStackState;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle;

/**
 * backport: Sophisticated Core's linked storage ({@code LinkedStorageStackLifecycle}) only exists
 * since 1.5.x; modpacks still ship 1.4.x (TerraFirmaCraft Rebirth 2 has 1.4.81). Referencing it
 * straight from a mixin made the mixin fail to attach there, which took Sophisticated Backpacks and
 * the whole game start down with it. The reference now lives in {@link Linked}, a class the JVM only
 * loads when the API is present; without it nothing is a linked endpoint.
 */
public final class SophisticatedLinkedStorage {

    private static final boolean PRESENT = probe();

    // backport-fix: BF-082 the LINKED_STORAGE_ENDPOINT data component is just as new (1.5.x)
    private static final boolean COMPONENT_PRESENT = probeComponent();

    private SophisticatedLinkedStorage() {}

    /** True when {@code backpack} is only a linked endpoint whose items live elsewhere. */
    public static boolean isEndpoint(ItemStack backpack) {
        return PRESENT && Linked.isEndpoint(backpack);
    }

    /**
     * backport-fix: BF-082 — true when {@code stack} carries Sophisticated Core's linked storage
     * endpoint component. Read straight from {@code ModCoreDataComponents} it threw
     * NoSuchFieldError on Core 1.4.x every time a backpack was looked at: creative search (its
     * tooltips), and every tick for a mob wearing a backpack (Sophisticated Backpacks gives them
     * to zombies, creepers...), which froze and crashed the game in such worlds.
     */
    public static boolean hasEndpointComponent(ItemStack stack) {
        return COMPONENT_PRESENT && Linked.hasEndpointComponent(stack);
    }

    /**
     * backport: the same check for any component holder (an ItemStack or an ItemStackTemplate), for
     * the item interior adapter (SophisticatedItemInteriors), which sees both.
     */
    public static boolean hasEndpointComponent(
            net.minecraft.core.component.DataComponentHolder holder) {
        return COMPONENT_PRESENT && Linked.hasEndpointComponent(holder);
    }

    private static boolean probeComponent() {
        try {
            net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents.class.getField(
                    "LINKED_STORAGE_ENDPOINT");
            return true;
        } catch (NoSuchFieldException | LinkageError e) {
            return false;
        }
    }

    private static boolean probe() {
        try {
            Class.forName(
                    "net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageStackLifecycle",
                    false,
                    SophisticatedLinkedStorage.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static final class Linked {
        static boolean hasEndpointComponent(net.minecraft.core.component.DataComponentHolder stack) {
            return stack.has(
                    net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents
                            .LINKED_STORAGE_ENDPOINT
                            .get());
        }

        static boolean isEndpoint(ItemStack backpack) {
            return LinkedStorageStackLifecycle.classifyEndpoint(backpack)
                    == LinkedStorageEndpointStackState.ENDPOINT;
        }
    }
}
