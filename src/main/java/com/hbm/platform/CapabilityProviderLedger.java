// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.registration.RegistryHandle;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CapabilityProviderLedger {

    private final Map<ResourceLocation, Reference2IntOpenHashMap<Object>> registrations =
            new LinkedHashMap<>();

    public void blocks(ResourceLocation lookup, Block... blocks) {
        Reference2IntOpenHashMap<Object> keys = keys(lookup);
        for (Block block : blocks) keys.addTo(block, 1);
    }

    public void blockEntity(ResourceLocation lookup, BlockEntityType<?> type) {
        keys(lookup).addTo(type, 1);
    }

    private Reference2IntOpenHashMap<Object> keys(ResourceLocation lookup) {
        return registrations.computeIfAbsent(lookup, id -> new Reference2IntOpenHashMap<>());
    }

    public Map<ResourceLocation, Reference2IntMap<Block>> providersPerBlock() {
        Map<ResourceLocation, Reference2IntMap<Block>> out = new LinkedHashMap<>();
        registrations.forEach(
                (lookup, keys) -> {
                    Reference2IntOpenHashMap<Block> perBlock = new Reference2IntOpenHashMap<>();
                    for (Reference2IntMap.Entry<Object> entry : keys.reference2IntEntrySet()) {
                        if (entry.getKey() instanceof BlockEntityType<?> type) {
                            for (RegistryHandle<? extends Block> handle :
                                    Services.REGISTRAR.blocks()) {
                                if (type.isValid(handle.get().defaultBlockState())) {
                                    perBlock.addTo(handle.get(), entry.getIntValue());
                                }
                            }
                        } else {
                            perBlock.addTo((Block) entry.getKey(), entry.getIntValue());
                        }
                    }
                    out.put(lookup, perBlock);
                });
        return out;
    }
}
