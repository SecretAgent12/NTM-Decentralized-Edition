// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.hazard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;

/**
 * backport: new helper. In 26.x the Sophisticated saved-data contents expose a decoded
 * {@code inventory().stacks()} list; Sophisticated 1.21.1 (BackpackStorage / ItemContentsStorage) keeps
 * the raw contents CompoundTag, whose "inventory" tag is InventoryHandler.serializeNBT: {"Items": [
 * {Slot, id, count, components}], "Size"} with oversized counts (CodecHelper.OVERSIZED_ITEM_STACK_CODEC).
 * This decodes that tag the way InventoryHandler.deserializeNBT does. The decoded list is cached per
 * storage UUID and side and reused while the "inventory" tag is the same object (InventoryHandler
 * .saveInventory and the client contents payloads always put a new tag), so the per-tick live-storage
 * hazard check does not re-decode an unchanged inventory.
 */
final class SophisticatedStoredStacks {
    private record Decoded(Tag inventory, List<ItemStack> stacks) {}

    private static final Map<UUID, Decoded> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Decoded> CLIENT = new ConcurrentHashMap<>();

    private SophisticatedStoredStacks() {}

    static List<ItemStack> stacks(UUID id, CompoundTag contents) {
        Map<UUID, Decoded> cache =
                Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER ? SERVER : CLIENT;
        Tag inventory = contents.get(InventoryHandler.INVENTORY_TAG);
        if (!(inventory instanceof CompoundTag inventoryTag)) {
            cache.remove(id);
            return List.of();
        }
        Decoded decoded = cache.get(id);
        if (decoded != null && decoded.inventory() == inventory) return decoded.stacks();
        Optional<RegistryAccess> registries = RegistryHelper.getRegistryAccess();
        if (registries.isEmpty()) return List.of();
        var ops = registries.get().createSerializationContext(NbtOps.INSTANCE);
        ListTag items = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
        List<ItemStack> stacks = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            CodecHelper.OVERSIZED_ITEM_STACK_CODEC
                    .parse(ops, items.getCompound(i))
                    .result()
                    .ifPresent(stacks::add);
        }
        List<ItemStack> result = List.copyOf(stacks);
        cache.put(id, new Decoded(inventory, result));
        return result;
    }
}
