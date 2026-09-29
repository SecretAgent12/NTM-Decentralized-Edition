// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;

/**
 * Backport of 26.x's net.minecraft.world.level.storage.ValueInput onto 1.21.1.
 *
 * In 26.x block entities, entities and saved data read their state through this
 * interface instead of a raw CompoundTag. 1.21.1 has no such thing, so this is
 * the same surface implemented over a CompoundTag (TagValueInput), which lets
 * every one of ntm-next's ~340 load methods compile unchanged.
 *
 * Semantics follow 26.x: getXOr returns the default when the key is missing OR
 * holds the wrong type; the Optional getters are empty in those cases.
 */
public interface ValueInput {
    <T> Optional<T> read(String key, Codec<T> codec);

    <T> Optional<T> read(MapCodec<T> codec);

    Optional<ValueInput> child(String key);

    ValueInput childOrEmpty(String key);

    Optional<ValueInputList> childrenList(String key);

    ValueInputList childrenListOrEmpty(String key);

    <T> Optional<TypedInputList<T>> list(String key, Codec<T> codec);

    <T> TypedInputList<T> listOrEmpty(String key, Codec<T> codec);

    boolean getBooleanOr(String key, boolean fallback);

    byte getByteOr(String key, byte fallback);

    int getShortOr(String key, short fallback);

    Optional<Integer> getInt(String key);

    int getIntOr(String key, int fallback);

    long getLongOr(String key, long fallback);

    Optional<Long> getLong(String key);

    float getFloatOr(String key, float fallback);

    double getDoubleOr(String key, double fallback);

    Optional<String> getString(String key);

    String getStringOr(String key, String fallback);

    Optional<int[]> getIntArray(String key);

    HolderLookup.Provider lookup();

    interface ValueInputList extends Iterable<ValueInput> {
        boolean isEmpty();

        Stream<ValueInput> stream();
    }

    interface TypedInputList<T> extends Iterable<T> {
        boolean isEmpty();

        Stream<T> stream();
    }
}
