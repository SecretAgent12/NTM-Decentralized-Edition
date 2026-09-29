// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/** 26.x {@code ExtraCodecs.LateBoundIdMapper}: id <-> value table filled at registration time. */
public final class IdMapper<V> {
    private final Map<ResourceLocation, V> byId = new ConcurrentHashMap<>();
    private final Map<V, ResourceLocation> byValue = new IdentityHashMap<>();

    public synchronized IdMapper<V> put(ResourceLocation id, V value) {
        byId.put(id, value);
        byValue.put(value, id);
        return this;
    }

    public V get(ResourceLocation id) {
        return byId.get(id);
    }

    public synchronized ResourceLocation getId(V value) {
        return byValue.get(value);
    }

    public Codec<V> codec(Codec<ResourceLocation> idCodec) {
        return idCodec.flatXmap(
                id -> {
                    V v = byId.get(id);
                    return v != null ? DataResult.success(v) : DataResult.error(() -> "Unknown type: " + id);
                },
                v -> {
                    ResourceLocation id = getId(v);
                    return id != null ? DataResult.success(id) : DataResult.error(() -> "Unregistered: " + v);
                });
    }
}
