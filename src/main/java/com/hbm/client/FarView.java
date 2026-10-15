// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

/**
 * backport: upstream reports the Flywheel LOD terrain distance ({@code
 * dev.engine_room.flywheel.api.lod.LodTerrain}, a 26.x Flywheel feature) to the server so far
 * entities stay tracked for LOD viewers. Flywheel 1.0.7 for 1.21.1 has no LOD terrain, so the
 * client never sees past its render distance: the far view stays 0 (the server default, as with
 * LOD off upstream) and nothing is reported.
 */
public final class FarView {
    private FarView() {}

    public static void init() {}

    public static void joined() {}
}
