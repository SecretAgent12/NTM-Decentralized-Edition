// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import net.minecraft.world.entity.LivingEntity;

// Flywheel overlay-texture (u, v) helpers over vanilla's LIVE overlay texture (flw_overlayTex/T1):
// rows 0-7 = solid red hurt band; rows 8-15 = per-column white gradient. Hence u = column (white-flash
// intensity), v = row (band), mirroring vanilla OverlayTexture.u(progress) and v(hurt).
// backport: 1.21.1's overlay texture has the same layout (NO_OVERLAY = pack(0, 10)).
public final class OverlayTexture {
    /**
     * Row of the white-flash band; at column 0 there the overlay alpha is 1 (keeps the original colour).
     */
    public static final int WHITE_OVERLAY_V = 10;
    public static final int RED_OVERLAY_V = 3;
    public static final int WHITE_MAX_U = 15;
    public static final int NO_OVERLAY = pack(0, WHITE_OVERLAY_V);
    public static final int HURT = pack(0, RED_OVERLAY_V);

    private OverlayTexture() {
    }

    public static int forEntity(LivingEntity entity) {
        return (entity.hurtTime > 0 || entity.deathTime > 0) ? HURT : NO_OVERLAY;
    }

    public static int whitePack(float intensity) {
        int u = (int) (Math.min(Math.max(intensity, 0.0F), 1.0F) * WHITE_MAX_U);
        return pack(u, WHITE_OVERLAY_V);
    }

    public static int pack(int u, int v) {
        return u | (v << 16);
    }

    public static int u(int packed) {
        return packed & 0xFFFF;
    }

    public static int v(int packed) {
        return (packed >> 16) & 0xFFFF;
    }
}
