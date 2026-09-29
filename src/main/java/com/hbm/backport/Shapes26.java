// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import java.util.function.IntFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 26.x Block shape helpers (1.21.2+). column(w, minY, maxY) is a box of width w centred
 * on the block; boxes(max, f) is f(0..max) -- 1.21.1's SnowLayerBlock spells out the
 * same nine layer shapes by hand.
 */
public final class Shapes26 {

    private Shapes26() {}

    public static VoxelShape column(double width, double minY, double maxY) {
        double half = width / 2.0;
        return Block.box(8.0 - half, minY, 8.0 - half, 8.0 + half, maxY, 8.0 + half);
    }

    public static VoxelShape column(double sizeX, double sizeZ, double minY, double maxY) {
        return Block.box(8.0 - sizeX / 2.0, minY, 8.0 - sizeZ / 2.0, 8.0 + sizeX / 2.0, maxY, 8.0 + sizeZ / 2.0);
    }

    public static VoxelShape cube(double size) {
        return column(size, 8.0 - size / 2.0, 8.0 + size / 2.0);
    }

    public static VoxelShape[] boxes(int max, IntFunction<VoxelShape> shape) {
        VoxelShape[] out = new VoxelShape[max + 1];
        for (int i = 0; i <= max; i++) out[i] = shape.apply(i);
        return out;
    }
}
