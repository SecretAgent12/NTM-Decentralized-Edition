// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 26.x {@code net.minecraft.client.renderer.state.level.BlockBreakingRenderState}. */
public record BlockBreakingRenderState(BlockPos blockPos, BlockState blockState, int progress) {}
