// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import dev.engine_room.flywheel.api.instance.InstanceType;
import java.nio.ByteBuffer;
import java.util.function.LongConsumer;
import org.lwjgl.system.MemoryUtil;

/**
 * backport: CrankShaft instances write straight into their GPU slot ({@code slabPtr()}); Flywheel 1.0 instead asks
 * each instance to write itself through its type's {@code InstanceWriter}. A Slab is a per-instance off-heap copy of
 * the slot: setters write into it at CrankShaft's offsets (Flywheel 1.0 packs layouts identically), the type's writer
 * copies it into the real slot on upload. Seeded like CrankShaft's slot (the type's seed, else zeros).
 */
public final class Slab {
    private final ByteBuffer buffer; // keeps the memory alive; freed with the instance by the GC
    public final long address;
    public final int size;

    public Slab(InstanceType<?> type) {
        size = type.layout().byteSize();
        buffer = ByteBuffer.allocateDirect(Math.max(size, 16));
        address = MemoryUtil.memAddress(buffer);
        LongConsumer seed = type instanceof SimpleInstanceType<?> simple ? simple.seed() : null;
        if (seed != null) seed.accept(address);
    }

    public void copyTo(long ptr) {
        MemoryUtil.memCopy(address, ptr, size);
    }

    /** Implemented by instances that carry a {@link Slab}. */
    public interface Holder {
        Slab slab();
    }
}
