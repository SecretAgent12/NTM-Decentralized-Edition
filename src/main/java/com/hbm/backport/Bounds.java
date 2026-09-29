// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.mojang.serialization.DataResult;
import java.util.Optional;
import net.minecraft.advancements.critereon.MinMaxBounds;

/**
 * 26.x MinMaxBounds.Bounds: an optional lower and upper bound. 1.21.1's MinMaxBounds.Ints
 * keeps min/max itself and has no separate bounds record.
 */
public record Bounds<T extends Number & Comparable<T>>(Optional<T> min, Optional<T> max) {

    public static Bounds<Integer> of(MinMaxBounds.Ints ints) {
        return new Bounds<>(ints.min(), ints.max());
    }

    public static <T extends Number & Comparable<T>> DataResult<Bounds<T>> validateSwappedBoundsInCodec(Bounds<T> b) {
        return b.min.isPresent() && b.max.isPresent() && b.min.get().compareTo(b.max.get()) > 0
                ? DataResult.error(() -> "Swapped bounds in range: " + b.min.get() + " > " + b.max.get())
                : DataResult.success(b);
    }
}
