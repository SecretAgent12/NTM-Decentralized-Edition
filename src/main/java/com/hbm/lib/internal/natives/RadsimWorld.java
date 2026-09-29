// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.function.LongConsumer;

/** Backport stub: NTM: NEXT's native (FFM) code is not part of this build. */
public final class RadsimWorld implements AutoCloseable {


    private static final int EVENT_HEADER_BYTES = 16;


    public static final int MASK_WORDS_PER_SECTION = 64;


    private static final class EntryScratch {


    }


    public RadsimWorld(
            int dimension, long seed, double minBound, int threads, int sectionsPerChunk) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public long handle() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public RadsimTables tables() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void setParams(
            double diffusionDt,
            double uniformExchange,
            double retentionDt,
            long fogProbU64,
            long destroyProbU64,
            double fogThreshold,
            double eps,
            double maxValue) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static final int COLUMN_PATH_FORCE_SCALAR = 1;

    public static final int COLUMN_PATH_DISABLED = 2;


    public void columnPathFlags(int flags) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void forceScalarColumns(boolean force) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static final int KERNEL_BYPASS_COEFFICIENT_CACHE = 1;


    public void debugKernelFlags(int flags) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void bypassCoefficientCache(boolean force) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public int sectionsPerChunk() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public long[] lastStepProfileNanos() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void chunkLoaded(long ck) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void chunkUnloaded(long ck) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void chunkRemoved(long ck) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public int chunkId(long ck) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public int step(long epochSalt, int epoch, int permBits) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void drainEvents(int bytes, LongConsumer fog, LongConsumer destroy) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public long droppedEventSteps() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public long droppedEventBytes() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public double density(long ck, int sy) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void submitSectionSources(
            long[] keys,
            int[] entryCounts,
            short[] pockets,
            int[] multiplicities,
            double[] emissions,
            double[] saturations,
            int sectionCount,
            int entryTotal) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static final int FEATURE_DIFFUSIVITY_TRANSPORT = 1;


    public void setFeatureFlags(int flags) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public long diffusivityFailures() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public long[] validationFailures() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void submitSectionDiffusivity(
            long[] keys, int[] pocketCounts, float[] values, int sectionCount, int valueTotal) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private void growDiffusivity(int sectionCount, int valueTotal) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private void growSources(int sectionCount, int entryTotal) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void submitDirtySections(long[] keys, long[] maskWords, int count) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void setRad(long sectionKey, int local, double value) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void addRad(long sectionKey, int local, double amount) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void emitRad(long sectionKey, int local, double emission, double saturation) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private void stageEdit(long sectionKey, int local, double add, byte flags, double saturation) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public double localDensity(long sectionKey, int local) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private EntryScratch growEntryScratch(int rows) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public int dumpChunkEntries(long ck, int[] sypiOut, double[] densityOut) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private synchronized int readChunkEntries(long ck, EntryScratch scratch) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public void loadChunkEntries(long ck, int count, int[] sypi, double[] density) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private void growEdits(int count) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private void growDirty(int count) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private void growEvents(int needed) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @Override
    public void close() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }
}
