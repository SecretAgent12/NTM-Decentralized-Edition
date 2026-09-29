// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.Nullable;

/**
 * Backport of 26.x's net.minecraft.core.component.DataComponentGetter: the
 * read-only view of components that block entities receive when they are placed
 * from an item. 1.21.1 hands BlockEntity.DataComponentInput instead;
 * BlockEntityCompat adapts one to the other.
 */
public interface DataComponentGetter {
    <T> @Nullable T get(DataComponentType<? extends T> type);

    default <T> T getOrDefault(DataComponentType<? extends T> type, T fallback) {
        T value = get(type);
        return value != null ? value : fallback;
    }
}
