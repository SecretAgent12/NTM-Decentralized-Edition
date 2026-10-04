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

    private SophisticatedLinkedStorage() {}

    /** True when {@code backpack} is only a linked endpoint whose items live elsewhere. */
    public static boolean isEndpoint(ItemStack backpack) {
        return PRESENT && Linked.isEndpoint(backpack);
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
        static boolean isEndpoint(ItemStack backpack) {
            return LinkedStorageStackLifecycle.classifyEndpoint(backpack)
                    == LinkedStorageEndpointStackState.ENDPOINT;
        }
    }
}
