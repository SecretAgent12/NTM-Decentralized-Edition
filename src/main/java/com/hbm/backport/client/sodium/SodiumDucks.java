// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.sodium;

import net.minecraft.world.level.BlockAndTintGetter;

/**
 * backport: the 26.x Sodium compat mixins (com.hbm.mixin.compat.sodium) use Sodium's classes
 * directly (ntm-next compiles against Sodium). The backport has no Sodium on its compile
 * classpath, so the mixins target Sodium by name only and reach the few Sodium members they need
 * through these interfaces, which small mixins add to the Sodium classes at runtime (only when
 * Sodium is installed; ClientCompatPlugin skips the sodium mixins otherwise). Nothing here
 * mentions a Sodium type, so the mod compiles and runs without Sodium.
 *
 * <p>Checked against Sodium 0.8.13 for 1.21.1 (identical in 0.8.12).
 */
public final class SodiumDucks {
    private SodiumDucks() {}

    /**
     * Sodium's {@code render.frapi.mesh.QuadViewImpl} (the quad its block renderer lights). Its
     * own {@code public final int tag()} implements this (duck interface, QuadTagMixin); the
     * backport stores the HBM light origin of a quad in the tag (QuadImportMixin).
     */
    public interface QuadTag {
        int tag();
    }

    /**
     * Sodium's {@code model.light.data.LightDataAccess}; its own {@code public BlockAndTintGetter
     * getLevel()} implements this (duck interface, LightCacheMixin).
     */
    public interface LightCache {
        BlockAndTintGetter getLevel();
    }

    /** Sodium's {@code model.light.data.QuadLightData} (QuadLightDataMixin): its lightmap array. */
    public interface QuadLightData {
        int[] hbm$lightmaps();
    }
}
