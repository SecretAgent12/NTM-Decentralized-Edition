// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 26.x {@code net.minecraft.client.renderer.state.level.QuadParticleRenderState}: camera-relative
 * billboard quads collected per {@link SingleQuadParticle.Layer}.
 *
 * <p>backport: {@link #submit} draws each layer's quads as custom geometry with the layer's 1.21.1
 * RenderType (opaque layers first, then translucent, each in first-use order). Vertex layout follows
 * 1.21.1 {@code SingleQuadParticle.renderRotatedQuad}.
 */
public class QuadParticleRenderState implements ParticleGroupRenderState {

    private static final int STRIDE = 14;

    private final Map<SingleQuadParticle.Layer, Batch> batches = new IdentityHashMap<>();
    private final List<SingleQuadParticle.Layer> order = new ArrayList<>();

    public void add(
            SingleQuadParticle.Layer layer,
            float x,
            float y,
            float z,
            float xRot,
            float yRot,
            float zRot,
            float wRot,
            float scale,
            float u0,
            float u1,
            float v0,
            float v1,
            int color,
            int lightCoords) {
        Batch b = batches.get(layer);
        if (b == null) {
            b = new Batch();
            batches.put(layer, b);
            order.add(layer);
        }
        b.add(x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, color, lightCoords);
    }

    public boolean isEmpty() {
        return order.isEmpty();
    }

    @Override
    public void clear() {
        batches.clear();
        order.clear();
    }

    @Override
    public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
        for (int pass = 0; pass < 2; pass++) {
            for (SingleQuadParticle.Layer layer : order) {
                if (layer.translucent() != (pass == 1)) continue;
                Batch b = batches.get(layer);
                if (b.count == 0) continue;
                collector.submitCustomGeometry(
                        new PoseStack(), layer.renderType(), (pose, buffer) -> b.emit(buffer, pose.pose()));
            }
        }
    }

    /** backport: writes every quad, regardless of layer, into one consumer (1.21.1 per-particle path). */
    void emitAll(VertexConsumer buffer) {
        for (SingleQuadParticle.Layer layer : order) batches.get(layer).emit(buffer, null);
    }

    private static final class Batch {
        float[] data = new float[STRIDE * 16];
        int count;

        void add(
                float x,
                float y,
                float z,
                float xRot,
                float yRot,
                float zRot,
                float wRot,
                float scale,
                float u0,
                float u1,
                float v0,
                float v1,
                int color,
                int light) {
            int i = count * STRIDE;
            if (i + STRIDE > data.length) data = Arrays.copyOf(data, data.length * 2);
            data[i] = x;
            data[i + 1] = y;
            data[i + 2] = z;
            data[i + 3] = xRot;
            data[i + 4] = yRot;
            data[i + 5] = zRot;
            data[i + 6] = wRot;
            data[i + 7] = scale;
            data[i + 8] = u0;
            data[i + 9] = u1;
            data[i + 10] = v0;
            data[i + 11] = v1;
            data[i + 12] = Float.intBitsToFloat(color);
            data[i + 13] = Float.intBitsToFloat(light);
            count++;
        }

        void emit(VertexConsumer buffer, Matrix4f matrix) {
            Quaternionf q = new Quaternionf();
            Vector3f v = new Vector3f();
            for (int n = 0; n < count; n++) {
                int i = n * STRIDE;
                float x = data[i], y = data[i + 1], z = data[i + 2];
                q.set(data[i + 3], data[i + 4], data[i + 5], data[i + 6]);
                float s = data[i + 7];
                float u0 = data[i + 8], u1 = data[i + 9], v0 = data[i + 10], v1 = data[i + 11];
                int color = Float.floatToRawIntBits(data[i + 12]);
                int light = Float.floatToRawIntBits(data[i + 13]);
                vertex(buffer, matrix, q, v, x, y, z, 1.0F, -1.0F, s, u1, v1, color, light);
                vertex(buffer, matrix, q, v, x, y, z, 1.0F, 1.0F, s, u1, v0, color, light);
                vertex(buffer, matrix, q, v, x, y, z, -1.0F, 1.0F, s, u0, v0, color, light);
                vertex(buffer, matrix, q, v, x, y, z, -1.0F, -1.0F, s, u0, v1, color, light);
            }
        }

        private static void vertex(
                VertexConsumer buffer,
                Matrix4f matrix,
                Quaternionf q,
                Vector3f v,
                float x,
                float y,
                float z,
                float xOff,
                float yOff,
                float size,
                float u,
                float vv,
                int color,
                int light) {
            v.set(xOff, yOff, 0.0F).rotate(q).mul(size).add(x, y, z);
            if (matrix != null) buffer.addVertex(matrix, v.x(), v.y(), v.z());
            else buffer.addVertex(v.x(), v.y(), v.z());
            buffer.setUv(u, vv).setColor(color).setLight(light);
        }
    }
}
