// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

/** backport: Flywheel 1.0's ExtraMemoryOps plus CrankShaft's scalar-component vector writers. */
public final class ExtraMemoryOps {
    private ExtraMemoryOps() {}

    public static void put4x8(long ptr, int value) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.put4x8(ptr, value);
    }

    public static void put2x16(long ptr, int value) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.put2x16(ptr, value);
    }

    public static void putVector2f(long ptr, Vector2fc vector) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.putVector2f(ptr, vector);
    }

    public static void putVector3f(long ptr, Vector3fc vector) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.putVector3f(ptr, vector);
    }

    public static void putVector4f(long ptr, Vector4fc vector) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.putVector4f(ptr, vector);
    }

    public static void putQuaternionf(long ptr, Quaternionfc quaternion) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.putQuaternionf(ptr, quaternion);
    }

    public static void putMatrix3f(long ptr, Matrix3fc matrix) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.putMatrix3f(ptr, matrix);
    }

    public static void putMatrix3fPadded(long ptr, Matrix3fc matrix) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.putMatrix3fPadded(ptr, matrix);
    }

    public static void putMatrix4f(long ptr, Matrix4fc matrix) {
        dev.engine_room.flywheel.lib.util.ExtraMemoryOps.putMatrix4f(ptr, matrix);
    }

    public static void putVector3f(long ptr, float x, float y, float z) {
        MemoryUtil.memPutFloat(ptr, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);
    }

    public static void putVector4f(long ptr, float x, float y, float z, float w) {
        MemoryUtil.memPutFloat(ptr, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);
        MemoryUtil.memPutFloat(ptr + 12, w);
    }
}
