// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 26.x EntityReference: an entity remembered by UUID, resolved lazily (and cached while
 * the entity is alive). Saved as the UUID, like 26.x.
 */
public final class EntityReference<T extends Entity> {

    private final UUID uuid;
    private WeakReference<T> cached;

    private EntityReference(UUID uuid, @Nullable T entity) {
        this.uuid = uuid;
        this.cached = new WeakReference<>(entity);
    }

    public static <T extends Entity> EntityReference<T> of(T entity) {
        return new EntityReference<>(entity.getUUID(), entity);
    }

    public static <T extends Entity> EntityReference<T> of(UUID uuid) {
        return new EntityReference<>(uuid, null);
    }

    public UUID getUUID() {
        return uuid;
    }

    public boolean matches(Entity entity) {
        return uuid.equals(entity.getUUID());
    }

    /** Resolves through `lookup` (e.g. ServerLevel::getEntity) when the cached entity is gone. */
    public @Nullable T getEntity(Function<UUID, ? extends Entity> lookup, Class<T> type) {
        T entity = cached.get();
        if (entity != null && !entity.isRemoved()) return entity;
        Entity found = lookup.apply(uuid);
        if (type.isInstance(found)) {
            T typed = type.cast(found);
            cached = new WeakReference<>(typed);
            return typed;
        }
        return null;
    }

    public static <T extends Entity> @Nullable EntityReference<T> read(ValueInput input, String key) {
        return input.read(key, UUIDUtil.CODEC).map(EntityReference::<T>of).orElse(null);
    }

    public static void store(@Nullable EntityReference<?> reference, ValueOutput output, String key) {
        if (reference != null) output.store(key, UUIDUtil.CODEC, reference.uuid);
    }
}
