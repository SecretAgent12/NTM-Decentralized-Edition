// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.noise;

import com.hbm.lib.internal.natives.NativeBindings;
import java.util.Objects;

/** Backport stub: NTM: NEXT's native (FFM) code is not part of this build. */
final class SimplexBatchNative implements SimplexBatch {


    private static final int MAX_CRITICAL_WORK = 256;

    private static final int MAX_CRITICAL_SAMPLES = 64;

    private static final double MAX_GRID_COORDINATE = 1.0E9D;


    SimplexBatchNative() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @Override
    public void accumulate(
            int[] perm,
            double[] gradX,
            double[] gradY,
            int tableOffset,
            double[] xs,
            double[] ys,
            double scale,
            double weight,
            double[] acc,
            int len) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @Override
    public void fractal(
            int[] perm,
            double[] gradX,
            double[] gradY,
            double[] xs,
            double[] ys,
            double[] out,
            int octaves,
            int len) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @Override
    public void fractalGrid(
            int[] perm,
            double[] gradX,
            double[] gradY,
            int firstX,
            int firstZ,
            int sizeX,
            int sizeZ,
            double scaleX,
            double scaleZ,
            double[] out,
            int octaves) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private static boolean gridCoordinatesSafe(
            int firstX, int firstZ, int sizeX, int sizeZ, double scaleX, double scaleZ) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private static boolean coordinateSafe(long coordinate, double scale) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private static void checkFractalTables(
            int[] perm, double[] gradX, double[] gradY, int octaves) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }
}
