// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * 26.x net.minecraft.world.level.CardinalLighting (BlockAndTintGetter#cardinalLighting()): the
 * directional shade of the six faces. 1.21.1 asks BlockAndTintGetter#getShade(direction, shade)
 * instead; {@link #of} samples it, {@link #shade} answers it for views written against 26.x.
 */
public record CardinalLighting(float down, float up, float north, float south, float west, float east) {

    /** Overworld-style shade (1.21.1 ClientLevel#getShade with constant ambient light off). */
    public static final CardinalLighting DEFAULT = new CardinalLighting(0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F);
    /** Nether-style shade (constant ambient light). */
    public static final CardinalLighting NETHER = new CardinalLighting(0.9F, 0.9F, 0.8F, 0.8F, 0.6F, 0.6F);
    public static final CardinalLighting FULL = new CardinalLighting(1F, 1F, 1F, 1F, 1F, 1F);

    public float byFace(Direction direction) {
        return switch (direction) {
            case DOWN -> down;
            case UP -> up;
            case NORTH -> north;
            case SOUTH -> south;
            case WEST -> west;
            case EAST -> east;
        };
    }

    /** 1.21.1 BlockAndTintGetter#getShade semantics: unshaded faces are full bright. */
    public float shade(Direction direction, boolean shade) {
        return shade ? byFace(direction) : 1.0F;
    }

    /** 26.x level.cardinalLighting() for a 1.21.1 level/view. */
    public static CardinalLighting of(BlockAndTintGetter level) {
        CardinalLighting sampled = new CardinalLighting(
                level.getShade(Direction.DOWN, true),
                level.getShade(Direction.UP, true),
                level.getShade(Direction.NORTH, true),
                level.getShade(Direction.SOUTH, true),
                level.getShade(Direction.WEST, true),
                level.getShade(Direction.EAST, true));
        if (sampled.equals(DEFAULT)) return DEFAULT;
        if (sampled.equals(NETHER)) return NETHER;
        return sampled;
    }
}
