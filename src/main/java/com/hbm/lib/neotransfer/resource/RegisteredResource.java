/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer.resource;

import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.ApiStatus;

/**
 * Helper interface for resources backed by a {@linkplain #value registry entry}.
 *
 * @param <T> The type of the backing registry entry.
 */
// backport: 26.x extends net.minecraft.core.TypedInstance<T>, which does not exist in 1.21.1;
// its accessors (typeHolder, tags, is(...)) are declared directly on this interface instead.
public interface RegisteredResource<T> extends Resource {
    /**
     * {@return the backing instance of the resource}
     */
    T value();

    /**
     * {@return the registry holder of the backing instance}
     */
    Holder<T> typeHolder();

    /**
     * {@return the tags of the backing registry entry}
     */
    default Stream<TagKey<T>> tags() {
        return typeHolder().tags();
    }

    /**
     * {@return {@code true} if the backing registry entry is in the given tag}
     */
    default boolean is(TagKey<T> tag) {
        return typeHolder().is(tag);
    }

    /**
     * {@return {@code true} if the backing registry entry is in the given holder set}
     */
    default boolean is(HolderSet<T> holders) {
        return holders.contains(typeHolder());
    }

    /**
     * {@return {@code true} if the backing instance is the given instance}
     */
    default boolean is(T type) {
        return value() == type;
    }

    /**
     * {@return {@code true} if the backing registry entry is the given holder}
     */
    default boolean is(Holder<T> holder) {
        return value() == holder.value();
    }

    /**
     * {@return {@code true} if the backing registry entry has the given key}
     */
    default boolean is(ResourceKey<T> key) {
        return typeHolder().is(key);
    }

    /**
     * @param predicate The predicate to perform the test.
     * @return {@code true} if the predicate's test returns {@code true} for the holder from {@link #typeHolder()}.
     */
    @ApiStatus.NonExtendable
    default boolean is(Predicate<Holder<T>> predicate) {
        return predicate.test(typeHolder());
    }
}
