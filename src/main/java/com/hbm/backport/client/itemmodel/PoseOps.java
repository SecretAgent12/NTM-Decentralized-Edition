// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.MatrixUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/**
 * 26.x {@code PoseStack.Pose} mutators (translate/rotate/scale/mulPose/setIdentity), which 1.21.1's
 * Pose lacks (they return the pose for chaining); rewritten to these statics by the backport. Same math as 1.21.1's PoseStack
 * methods. backport: Pose.trustedNormals is package-private and stays as it was (1.21.1 clears it
 * for non-uniform scales / non-orthonormal matrices, which only re-normalizes normals).
 */
public final class PoseOps {
    private PoseOps() {}

    public static PoseStack.Pose translate(PoseStack.Pose pose, float x, float y, float z) {
        pose.pose().translate(x, y, z);
        return pose;
    }

    public static PoseStack.Pose translate(PoseStack.Pose pose, Vector3fc v) {
        pose.pose().translate(v);
        return pose;
    }

    public static PoseStack.Pose scale(PoseStack.Pose pose, float x, float y, float z) {
        pose.pose().scale(x, y, z);
        if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
            if (x < 0F || y < 0F || z < 0F)
                pose.normal().scale(Math.signum(x), Math.signum(y), Math.signum(z));
        } else {
            pose.normal().scale(1F / x, 1F / y, 1F / z);
        }
        return pose;
    }

    public static PoseStack.Pose rotate(PoseStack.Pose pose, Quaternionfc rotation) {
        pose.pose().rotate(rotation);
        pose.normal().rotate(rotation);
        return pose;
    }

    public static PoseStack.Pose rotateAround(PoseStack.Pose pose, Quaternionfc rotation, float x, float y, float z) {
        pose.pose().rotateAround(rotation, x, y, z);
        pose.normal().rotate(rotation);
        return pose;
    }

    public static PoseStack.Pose mulPose(PoseStack.Pose pose, Matrix4fc matrix) {
        Matrix4f m = new Matrix4f(matrix);
        pose.pose().mul(m);
        if (!MatrixUtil.isPureTranslation(m)) {
            if (MatrixUtil.isOrthonormal(m)) pose.normal().mul(new Matrix3f(m));
            else pose.normal().set(pose.pose()).invert().transpose();
        }
        return pose;
    }

    public static PoseStack.Pose setIdentity(PoseStack.Pose pose) {
        pose.pose().identity();
        pose.normal().identity();
        return pose;
    }

    public static boolean isIdentity(PoseStack.Pose pose) {
        return (pose.pose().properties() & Matrix4fc.PROPERTY_IDENTITY) != 0;
    }

    public static PoseStack.Pose set(PoseStack.Pose pose, PoseStack.Pose other) {
        pose.pose().set(other.pose());
        pose.normal().set(other.normal());
        return pose;
    }
}
