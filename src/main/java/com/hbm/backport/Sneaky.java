// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

/**
 * Backport: rethrows any Throwable, checked or not, without declaring it.
 *
 * ntm-next did this with an inline `aload failure; athrow;` bytecode block
 * (tenon-asm). The generic-erasure trick below compiles to the same athrow.
 */
public final class Sneaky {
    private Sneaky() {}

    public static RuntimeException rethrow(Throwable failure) {
        return Sneaky.<RuntimeException>sneak(failure);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneak(Throwable failure) throws T {
        throw (T) failure;
    }
}
