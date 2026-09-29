// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 26.x {@code net.minecraft.client.renderer.state.level.BlockOutlineRenderState}.
 * backport: unverified: component set matches how NEXT uses it (pos, isTranslucent, highContrast) plus
 * the outlined shape; 1.21.1 draws the outline in LevelRenderer.renderHitOutline /
 * RenderHighlightEvent.Block, where these are built with {@link #of}.
 */
public record BlockOutlineRenderState(
        BlockPos pos, boolean isTranslucent, boolean highContrast, VoxelShape shape) {

    /** 1.21.1 has no high-contrast outline option; translucency follows the block's render layer. */
    public static BlockOutlineRenderState of(BlockPos pos, boolean translucent, VoxelShape shape) {
        return new BlockOutlineRenderState(pos, translucent, false, shape);
    }
}
