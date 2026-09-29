// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * 26.x {@code net.minecraft.client.renderer.SubmitNodeCollector}: an {@link
 * OrderedSubmitNodeCollector} that can hand out collectors for explicit draw orders.
 */
public interface SubmitNodeCollector extends OrderedSubmitNodeCollector {

    /**
     * 26.x draw-order buckets. backport: 1.21.1 draws immediately in submission order, which the tree
     * already uses as the order within a bucket; the immediate collector returns itself.
     */
    OrderedSubmitNodeCollector order(int order);

    @FunctionalInterface
    interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer buffer);
    }

    /** A collector drawing straight into a 1.21.1 buffer source. */
    static SubmitNodeCollector immediate(MultiBufferSource buffers) {
        return new ImmediateSubmitNodeCollector(buffers);
    }

    /** A fresh PoseStack whose top pose is a copy of {@code pose} (1.21.1 model APIs take stacks). */
    static PoseStack poseStackOf(PoseStack.Pose pose) {
        PoseStack ps = new PoseStack();
        PoseStack.Pose last = ps.last();
        last.pose().set(pose.pose());
        last.normal().set(pose.normal());
        return ps;
    }
}
