// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

/** backport: 26.x net.minecraft.util.ToFloatFunction is a plain functional interface (1.21.1's is not). */
@FunctionalInterface
public interface ToFloatFunction<C> {
    float applyAsFloat(C value);
}
