// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.misc;

import net.minecraft.world.level.block.state.BlockState;

/** 26.x {@code net.minecraft.client.color.block.BlockTintSources} (the part the tree uses). */
public final class BlockTintSources {

    private BlockTintSources() {}

    public static BlockTintSource constant(int color) {
        return new Constant(color);
    }

    private record Constant(int value) implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return value;
        }
    }
}
