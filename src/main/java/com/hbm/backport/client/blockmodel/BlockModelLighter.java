// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.blockmodel;

/**
 * 26.x net.minecraft.client.renderer.block.BlockModelLighter statics: its light/AO cache is the
 * 1.21.1 ModelBlockRenderer thread-local cache.
 */
public final class BlockModelLighter {
    private BlockModelLighter() {}

    public static void clearCache() {
        net.minecraft.client.renderer.block.ModelBlockRenderer.clearCache();
    }

    public static void enableCaching() {
        net.minecraft.client.renderer.block.ModelBlockRenderer.enableCaching();
    }
}
