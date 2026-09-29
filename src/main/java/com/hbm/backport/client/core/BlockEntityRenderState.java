// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState}. */
public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public BlockEntityType<?> blockEntityType = BlockEntityType.FURNACE; // backport: 26.x default is TEST_BLOCK (absent in 1.21.1)
    public int lightCoords;
    public ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress;

    /** The base extraction every 26.x block entity renderer runs (BlockEntityRenderer.super). */
    public static void extractBase(
            BlockEntity be,
            BlockEntityRenderState state,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        state.blockPos = be.getBlockPos();
        state.blockState = be.getBlockState();
        state.blockEntityType = be.getType();
        Level level = be.getLevel();
        int bridged = BlockEntityRenderBridge.pendingLight();
        state.lightCoords =
                bridged >= 0
                        ? bridged
                        : level != null ? LevelRenderer.getLightColor(level, be.getBlockPos()) : 0xF000F0;
        state.breakProgress = breakProgress;
    }
}
