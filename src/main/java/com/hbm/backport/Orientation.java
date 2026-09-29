// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

/**
 * Backport placeholder for 26.x's redstone Orientation, the extra argument
 * neighborChanged gained in 1.21.2. 1.21.1 has no such concept, and 26.x itself
 * passes null unless experimental redstone is on, so the bridges always pass
 * null and nothing ever constructs one of these.
 */
public final class Orientation {
    private Orientation() {}
}
