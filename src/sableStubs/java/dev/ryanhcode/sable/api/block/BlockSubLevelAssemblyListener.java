// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package dev.ryanhcode.sable.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compile-time stand-in for Sable's interface of the same name (Sable 2.x, 1.21.1): only its
 * signatures, so the optional Sable mixin compiles without the Sable jar. It is not packaged; at
 * run time the class comes from Sable itself, and without Sable the mixin is not applied.
 */
public interface BlockSubLevelAssemblyListener {
    default void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState,
            BlockPos oldPos, BlockPos newPos) {}

    void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState,
            BlockPos oldPos, BlockPos newPos);
}
