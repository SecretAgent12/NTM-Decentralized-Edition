// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.misc;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 26.x {@code net.minecraft.client.color.block.BlockTintSource}: one tint layer of a block
 * (the tint index is the position in the list the source is registered with). On 1.21.1 a
 * list of sources is turned into one {@link net.minecraft.client.color.block.BlockColor} by
 * {@link BlockTints#color}.
 */
public interface BlockTintSource {

    /** Colour without a world context (item/GUI rendering, fallback). ARGB. */
    int color(BlockState state);

    /** Colour of a placed block. */
    default int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return color(state);
    }

    /**
     * Colour of the block's terrain (breaking/landing) particles.
     * backport: 1.21.1 TerrainParticle asks the same BlockColor as the chunk mesher (tint
     * index 0, with level and pos), so on 1.21.1 this is never called separately; the adapter
     * uses {@link #colorInWorld} for both.
     */
    default int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return colorInWorld(state, level, pos);
    }

    /**
     * Block state properties the colour depends on; a change of one of them re-meshes the
     * section. Registered as 1.21.1 BlockColors "coloring states" by {@link BlockTints}.
     */
    default Set<Property<?>> relevantProperties() {
        return Set.of();
    }
}
