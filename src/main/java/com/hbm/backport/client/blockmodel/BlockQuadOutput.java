// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.blockmodel;

/** 26.x net.minecraft.client.renderer.block.BlockQuadOutput: receives each lit quad of a block. */
@FunctionalInterface
public interface BlockQuadOutput {
    void put(float x, float y, float z, BakedQuad quad, QuadInstance instance);
}
