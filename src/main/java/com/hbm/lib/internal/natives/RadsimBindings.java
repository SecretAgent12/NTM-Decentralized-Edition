// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

import java.lang.invoke.MethodHandle;
import org.lwjgl.system.NativeType;


/** Backport stub: NTM: NEXT's native (FFM) code is not part of this build. */
public final class RadsimBindings {


    private static final class Debug {


    }


    private static void debug() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private RadsimBindings() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int32_t")
    public static int buildFlags() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static boolean reproducesJavaMath() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static boolean validatesEveryStep() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static boolean hasDebugDump() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int32_t")
    public static int sectionSourceCount(
            @NativeType("uint64_t") long handle, @NativeType("int64_t") long sectionKey) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int64_t")
    public static long diffusivityFailures(@NativeType("uint64_t") long handle) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int32_t")
    public static int cpuProfile() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void ensureBound() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void configureRuntime(
            @NativeType("int32_t") boolean parallel, @NativeType("int32_t") int threads) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("uint64_t")
    public static long worldCreate(
            @NativeType("int32_t") int dim,
            @NativeType("int64_t") long seed,
            @NativeType("double") double minBound,
            @NativeType("int32_t") int sectionsPerChunk) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void worldSetParams(
            @NativeType("uint64_t") long handle,
            @NativeType("double") double diffusionDt,
            @NativeType("double") double uniformExchange,
            @NativeType("double") double retentionDt,
            @NativeType("uint64_t") long fogProbU64,
            @NativeType("uint64_t") long destroyProbU64,
            @NativeType("double") double fogThreshold,
            @NativeType("double") double eps,
            @NativeType("double") double maxValue) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void worldDestroy(@NativeType("uint64_t") long handle) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void chunkLoaded(
            @NativeType("uint64_t") long handle, @NativeType("int64_t") long ck) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void chunkUnloaded(
            @NativeType("uint64_t") long handle, @NativeType("int64_t") long ck) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void chunkRemoved(
            @NativeType("uint64_t") long handle, @NativeType("int64_t") long ck) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void worldSetFeatureFlags(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int flags) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("double")
    public static double queryLocalDensity(
            @NativeType("uint64_t") long handle,
            @NativeType("int64_t") long sectionKey,
            @NativeType("int32_t") int local) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int32_t")
    public static int chunkId(@NativeType("uint64_t") long handle, @NativeType("int64_t") long ck) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("uint64_t")
    public static long bufferPtr(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int which) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int64_t")
    public static long bufferCount(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int which) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int32_t")
    public static int bufferStride(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int which) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int32_t")
    public static int bufferGeneration(@NativeType("uint64_t") long handle) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int32_t")
    public static int sectionsPerChunk() {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    @NativeType("int32_t")
    public static int worldSectionsPerChunk(@NativeType("uint64_t") long handle) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void debugKernelFlags(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int flags) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    public static void columnPathFlags(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int flags) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }
;


    private static void invokeVoid(MethodHandle handle, int a, int b) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }


    private static void invokeVoidLL(MethodHandle handle, long a, long b) {
        throw new UnsupportedOperationException("native library disabled in the 1.21.1 backport (FFM is preview-only on Java 21)");
    }
}
