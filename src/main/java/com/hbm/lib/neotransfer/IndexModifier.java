/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer;

import com.hbm.lib.neotransfer.resource.Resource;

/**
 * Represents a function to directly mutate the resource and amount at a specific index of a {@link ResourceHandler}.
 */
@FunctionalInterface
public interface IndexModifier<T extends Resource> {
    /**
     * Overrides the resource and amount at the given index.
     *
     * @param index    The index to set the resource at.
     * @param resource The resource to set.
     * @param amount   The amount of the resource to set.
     */
    void set(int index, T resource, int amount);
}
