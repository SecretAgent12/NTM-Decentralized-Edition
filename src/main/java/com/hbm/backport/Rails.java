// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * 26.x BaseRailBlock.rotate(RailShape, Rotation) / mirror(RailShape, Mirror). 1.21.1 has
 * the same tables inside RailBlock.rotate/mirror (on block states); these are them.
 */
public final class Rails {

    private Rails() {}

    public static RailShape rotate(RailShape shape, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> switch (shape) {
                case ASCENDING_EAST -> RailShape.ASCENDING_WEST;
                case ASCENDING_WEST -> RailShape.ASCENDING_EAST;
                case ASCENDING_NORTH -> RailShape.ASCENDING_SOUTH;
                case ASCENDING_SOUTH -> RailShape.ASCENDING_NORTH;
                case SOUTH_EAST -> RailShape.NORTH_WEST;
                case SOUTH_WEST -> RailShape.NORTH_EAST;
                case NORTH_WEST -> RailShape.SOUTH_EAST;
                case NORTH_EAST -> RailShape.SOUTH_WEST;
                default -> shape;
            };
            case COUNTERCLOCKWISE_90 -> switch (shape) {
                case NORTH_SOUTH -> RailShape.EAST_WEST;
                case EAST_WEST -> RailShape.NORTH_SOUTH;
                case ASCENDING_EAST -> RailShape.ASCENDING_NORTH;
                case ASCENDING_WEST -> RailShape.ASCENDING_SOUTH;
                case ASCENDING_NORTH -> RailShape.ASCENDING_WEST;
                case ASCENDING_SOUTH -> RailShape.ASCENDING_EAST;
                case SOUTH_EAST -> RailShape.NORTH_EAST;
                case SOUTH_WEST -> RailShape.SOUTH_EAST;
                case NORTH_WEST -> RailShape.SOUTH_WEST;
                case NORTH_EAST -> RailShape.NORTH_WEST;
            };
            case CLOCKWISE_90 -> switch (shape) {
                case NORTH_SOUTH -> RailShape.EAST_WEST;
                case EAST_WEST -> RailShape.NORTH_SOUTH;
                case ASCENDING_EAST -> RailShape.ASCENDING_SOUTH;
                case ASCENDING_WEST -> RailShape.ASCENDING_NORTH;
                case ASCENDING_NORTH -> RailShape.ASCENDING_EAST;
                case ASCENDING_SOUTH -> RailShape.ASCENDING_WEST;
                case SOUTH_EAST -> RailShape.SOUTH_WEST;
                case SOUTH_WEST -> RailShape.NORTH_WEST;
                case NORTH_WEST -> RailShape.NORTH_EAST;
                case NORTH_EAST -> RailShape.SOUTH_EAST;
            };
            default -> shape;
        };
    }

    public static RailShape mirror(RailShape shape, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> switch (shape) {
                case ASCENDING_NORTH -> RailShape.ASCENDING_SOUTH;
                case ASCENDING_SOUTH -> RailShape.ASCENDING_NORTH;
                case SOUTH_EAST -> RailShape.NORTH_EAST;
                case SOUTH_WEST -> RailShape.NORTH_WEST;
                case NORTH_WEST -> RailShape.SOUTH_WEST;
                case NORTH_EAST -> RailShape.SOUTH_EAST;
                default -> shape;
            };
            case FRONT_BACK -> switch (shape) {
                case ASCENDING_EAST -> RailShape.ASCENDING_WEST;
                case ASCENDING_WEST -> RailShape.ASCENDING_EAST;
                case SOUTH_EAST -> RailShape.SOUTH_WEST;
                case SOUTH_WEST -> RailShape.SOUTH_EAST;
                case NORTH_WEST -> RailShape.NORTH_EAST;
                case NORTH_EAST -> RailShape.NORTH_WEST;
                default -> shape;
            };
            default -> shape;
        };
    }
}
