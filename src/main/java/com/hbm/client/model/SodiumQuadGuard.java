// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Field;

/**
 * backport-fix: BF-015 Sodium's chunk-mesh consumer (ChunkVertexConsumer, 0.8.12/0.8.13 for 1.21.1)
 * keeps its vertex index at 4 when its translucent-sorting collector discards a quad (a degenerate
 * clipped quad, for example): potentiallyEndVertex() returns before resetting the index, and the next
 * addVertex() throws ArrayIndexOutOfBoundsException, crashing the chunk builder. Section geometry is
 * only emitted when Flywheel's backend is off, so it showed up with {@code flywheel:off} + Sodium.
 * After every quad this resets the index if it was left at the end of a quad; any other consumer is
 * left alone.
 */
final class SodiumQuadGuard {
    private static final String CONSUMER = "net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkVertexConsumer";
    private static volatile Class<?> type;
    private static volatile Field index;
    private static volatile boolean unavailable;

    private SodiumQuadGuard() {}

    static void endQuad(VertexConsumer consumer) {
        if (unavailable) return;
        Class<?> cls = consumer.getClass();
        Class<?> known = type;
        if (known == null) {
            if (!cls.getName().equals(CONSUMER)) return;
            if (!resolve(cls)) return;
            known = cls;
        }
        if (cls != known) return;
        try {
            Field f = index;
            if (f.getInt(consumer) >= 4) f.setInt(consumer, 0);
        } catch (IllegalAccessException | RuntimeException e) {
            unavailable = true;
        }
    }

    private static synchronized boolean resolve(Class<?> cls) {
        if (type != null) return type == cls;
        try {
            Field f = cls.getDeclaredField("vertexIndex");
            f.setAccessible(true);
            index = f;
            type = cls;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            unavailable = true;
            return false;
        }
    }
}
