// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.backport.ISectionGeneration;
import com.hbm.backport.Sneaky;
import com.hbm.lib.internal.GenerationTracking;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class SectionGeneration {
    private static final GenerationTracking TRACKING = new GenerationTracking(advanceHandle());
    private static final MethodHandle CHANGED = TRACKING.invoker();

    private SectionGeneration() {}

    public static void changed(LevelChunkSection section) {
        try {
            CHANGED.invokeExact(section);
        } catch (Throwable failure) {
            throw Sneaky.rethrow(failure);
        }
    }

    public static long generation(LevelChunkSection section) {
        return ((ISectionGeneration) section).hbm$generation();
    }

    public static void acquire() {
        TRACKING.acquire();
    }

    public static void release() {
        TRACKING.release();
    }

    private static MethodHandle advanceHandle() {
        try {
            return MethodHandles.lookup()
                    .findStatic(
                            SectionGeneration.class,
                            "advance",
                            MethodType.methodType(void.class, LevelChunkSection.class));
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void advance(LevelChunkSection section) {
        ISectionGeneration generation = (ISectionGeneration) section;
        generation.hbm$setGeneration(generation.hbm$generation() + 1);
    }
}
