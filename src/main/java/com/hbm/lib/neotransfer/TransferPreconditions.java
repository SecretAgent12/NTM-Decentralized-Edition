/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer;

import com.hbm.lib.neotransfer.resource.Resource;

/**
 * Precondition checks useful for implementing {@link ResourceHandler}.
 */
public class TransferPreconditions {
    private TransferPreconditions() {}

    /**
     * Ensures the resource is non-empty, throws otherwise.
     *
     * @throws IllegalArgumentException when resource is empty.
     */
    public static void checkNonEmpty(Resource resource) {
        if (resource.isEmpty()) {
            throw new IllegalArgumentException("Expected resource to be non-empty: " + resource);
        }
    }

    /**
     * Ensures the value is non-negative, throws otherwise.
     *
     * @throws IllegalArgumentException when value is negative.
     */
    public static void checkNonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Expected value to be non-negative: " + value);
        }
    }

    /**
     * Ensures the resource is non-empty and the value is non-negative, throws otherwise.
     *
     * @throws IllegalArgumentException when resource is empty or value is negative.
     */
    public static void checkNonEmptyNonNegative(Resource resource, int value) {
        checkNonEmpty(resource);
        checkNonNegative(value);
    }
}
