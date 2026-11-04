// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.neoforge.hazard.SophisticatedStoredStacks;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestStorageContentsPayload;

final class SophisticatedStorageHazardTooltipSync {
    private SophisticatedStorageHazardTooltipSync() {}

    static void reset() {
        HazardTooltipRequests.reset();
    }

    static void request(ItemStack stack) {
        UUID id = contentsId(stack);
        if (id != null) request(id, stack);
    }

    static List<ItemStack> requestNested(ItemStack stack, Set<UUID> visited) {
        UUID id = contentsId(stack);
        if (id == null || !visited.add(id)) return List.of();
        request(id, stack);
        // backport: 1.21.1 ItemContentsStorage.getOrCreateStorageContents returns the raw contents tag;
        // SophisticatedStoredStacks decodes it
        return SophisticatedStoredStacks.stacks(
                id, ItemContentsStorage.get().getOrCreateStorageContents(id));
    }

    static UUID contentsId(ItemStack stack) {
        return stack.getItem() instanceof StorageBlockItem
                ? stack.get(ModCoreDataComponents.STORAGE_UUID.get())
                : null;
    }

    private static void request(UUID id, ItemStack stack) {
        if (!HazardTooltipRequests.reserve(id, false)) return;
        // backport-fix: BF-132 the payload's constructor differs between Sophisticated Storage
        // versions (an ItemStackReference was added); SophisticatedContentsRequest picks it
        SophisticatedContentsRequest.send(RequestStorageContentsPayload.class, id, stack);
    }
}
