// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

// backport: Sophisticated Core 1.21.1 keeps each stored record as a CompoundTag (BackpackStorage /
// ItemContentsStorage map UUID -> CompoundTag), not a 26.x ContainerContents with live InventoryData
// and UpgradeData. Every InventoryHandler / UpgradeHandler bound to a record shares that tag as its
// contentsNbt, so the tag stands in for the record, and the inventory / upgrade identities 26.x took
// from InventoryData / UpgradeData are derived from it.
public interface SophisticatedContentsStorage {
    @Nullable CompoundTag storedContents(UUID id);

    static Object inventoryIdentity(CompoundTag contents) {
        return contents;
    }

    static Object upgradesIdentity(CompoundTag contents) {
        return Identities.UPGRADES.computeIfAbsent(contents, key -> new Object());
    }

    final class Identities {
        // weak keys compare by identity: one marker per record tag, gone with the tag
        private static final Map<CompoundTag, Object> UPGRADES =
                new MapMaker().weakKeys().makeMap();

        private Identities() {}
    }
}
