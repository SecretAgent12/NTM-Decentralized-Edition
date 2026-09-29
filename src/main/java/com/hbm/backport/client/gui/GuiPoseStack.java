// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2fc;

/**
 * The pose of a {@link GuiGraphicsExtractor}: the 1.21.1 GUI {@link PoseStack} it shares with the
 * vanilla GuiGraphics it wraps (every PoseStack call is delegated, so vanilla drawing code sees the
 * same stack), plus the 26.x {@code Matrix3x2fStack} calls the tree makes on {@code
 * graphics.pose()} (pushMatrix/popMatrix/translate(x, y)/scale(x, y)/rotate(angle)), mapped onto
 * the same stack as 2D transforms (z untouched).
 */
public class GuiPoseStack extends PoseStack {

    private final PoseStack base;

    public GuiPoseStack(PoseStack base) {
        this.base = base;
    }

    /** The shared vanilla stack. */
    public PoseStack base() {
        return base;
    }

    // ---- PoseStack, delegated ----------------------------------------------------------------

    @Override
    public void translate(double x, double y, double z) {
        base.translate(x, y, z);
    }

    @Override
    public void translate(float x, float y, float z) {
        base.translate(x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        base.scale(x, y, z);
    }

    @Override
    public void mulPose(Quaternionf rotation) {
        base.mulPose(rotation);
    }

    @Override
    public void rotateAround(Quaternionf rotation, float x, float y, float z) {
        base.rotateAround(rotation, x, y, z);
    }

    @Override
    public void pushPose() {
        base.pushPose();
    }

    @Override
    public void popPose() {
        base.popPose();
    }

    @Override
    public PoseStack.Pose last() {
        return base.last();
    }

    @Override
    public boolean clear() {
        return base.clear();
    }

    @Override
    public void setIdentity() {
        base.setIdentity();
    }

    @Override
    public void mulPose(Matrix4f pose) {
        base.mulPose(pose);
    }

    // ---- 26.x Matrix3x2fStack -----------------------------------------------------------------

    public GuiPoseStack pushMatrix() {
        base.pushPose();
        return this;
    }

    public GuiPoseStack popMatrix() {
        base.popPose();
        return this;
    }

    public GuiPoseStack translate(float x, float y) {
        base.translate(x, y, 0F);
        return this;
    }

    public GuiPoseStack translate(Vector2fc offset) {
        return translate(offset.x(), offset.y());
    }

    public GuiPoseStack scale(float x, float y) {
        base.scale(x, y, 1F);
        return this;
    }

    public GuiPoseStack scale(float xy) {
        return scale(xy, xy);
    }

    public GuiPoseStack rotate(float angle) {
        base.mulPose(Axis.ZP.rotation(angle));
        return this;
    }

    public GuiPoseStack rotateAbout(float angle, float x, float y) {
        base.rotateAround(Axis.ZP.rotation(angle), x, y, 0F);
        return this;
    }

    public GuiPoseStack mul(Matrix3x2fc m) {
        Matrix4f m4 = new Matrix4f(
                m.m00(), m.m01(), 0F, 0F,
                m.m10(), m.m11(), 0F, 0F,
                0F, 0F, 1F, 0F,
                m.m20(), m.m21(), 0F, 1F);
        base.mulPose(m4);
        return this;
    }

    public GuiPoseStack identity() {
        base.setIdentity();
        return this;
    }

    /** The 2D part (x/y rows) of the current pose, as 26.x hands out {@code new Matrix3x2f(pose())}. */
    public Matrix3x2f matrix2d() {
        Matrix4f m = base.last().pose();
        return new Matrix3x2f(m.m00(), m.m01(), m.m10(), m.m11(), m.m30(), m.m31());
    }

    /** The z translation of the current pose (1.21.1 GUI layering; 26.x has none). */
    public float z() {
        return base.last().pose().m32();
    }
}
