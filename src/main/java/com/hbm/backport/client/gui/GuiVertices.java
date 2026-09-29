// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

/** 26.x GUI vertex helpers missing from 1.21.1's VertexConsumer. */
public final class GuiVertices {

    private GuiVertices() {}

    /** 26.x {@code VertexConsumer.addVertexWith2DPose(pose, x, y)}: a GUI vertex at z 0. */
    public static VertexConsumer addVertexWith2DPose(VertexConsumer buffer, Matrix3x2fc pose, float x, float y) {
        Vector2f p = pose.transformPosition(x, y, new Vector2f());
        return buffer.addVertex(p.x, p.y, 0F);
    }
}
