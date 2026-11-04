// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.neoforge.hazard.SophisticatedLinkedStorage;
import com.hbm.neoforge.hazard.SophisticatedStoredStacks;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.network.RequestBackpackInventoryContentsPayload;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

final class SophisticatedBackpackHazardTooltipSync {
    private SophisticatedBackpackHazardTooltipSync() {}

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
        // backport: 1.21.1 BackpackStorage returns the raw contents tag; SophisticatedStoredStacks decodes it
        return SophisticatedStoredStacks.stacks(
                id, BackpackStorage.get().getOrCreateBackpackContents(id));
    }

    static UUID contentsId(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem)
                // backport-fix: BF-082 the component only exists on Sophisticated Core 1.5+
                || SophisticatedLinkedStorage.hasEndpointComponent(stack)) return null;
        return stack.get(ModCoreDataComponents.STORAGE_UUID.get());
    }

    private static void request(UUID id, ItemStack stack) {
        if (!HazardTooltipRequests.reserve(id, true)) return;
        // backport-fix: BF-132 the payload's constructor differs between Sophisticated Backpacks
        // versions (3.26.18 added an ItemStackReference); SophisticatedContentsRequest picks it
        SophisticatedContentsRequest.send(RequestBackpackInventoryContentsPayload.class, id, stack);
    }
}
