// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * 26.x com.mojang.blaze3d.vertex.QuadInstance: per-vertex colour (ARGB) and packed light of one
 * quad as it is drawn, plus an overlay. Instances produced by {@link ModelBlockRenderer} already
 * contain the quad's baked colours (tint x shade x AO x baked colour); instances made by callers
 * do not, and {@link #putBakedQuad}/{@link #putBlockBakedQuad} multiply the baked colours in for
 * those - so both uses draw what 26.x draws.
 *
 * <p>The 26.x VertexConsumer#putBakedQuad / #putBlockBakedQuad defaults are the static helpers
 * here (1.21.1 VertexConsumer has no such methods; a pass-90 rule rewrites the calls).
 */
public final class QuadInstance {
    private final int[] colors = {-1, -1, -1, -1};
    private final int[] light = new int[4];
    private int overlay = OverlayTexture.NO_OVERLAY;
    boolean bakedColorsApplied;

    public QuadInstance() {}

    public void setColor(int argb) {
        colors[0] = colors[1] = colors[2] = colors[3] = argb;
    }

    public void setColor(int vertex, int argb) {
        colors[vertex] = argb;
    }

    public int getColor(int vertex) {
        return colors[vertex];
    }

    public void multiplyColor(int argb) {
        for (int i = 0; i < 4; i++) colors[i] = multiply(colors[i], argb);
    }

    public void setLightCoords(int packed) {
        light[0] = light[1] = light[2] = light[3] = packed;
    }

    public void setLightCoords(int vertex, int packed) {
        light[vertex] = packed;
    }

    public int getLightCoords(int vertex) {
        return light[vertex];
    }

    /** The vertex light with the block component raised to at least {@code emission}. */
    public int getLightCoordsWithEmission(int vertex, int emission) {
        return withEmission(light[vertex], emission);
    }

    public void setOverlayCoords(int packed) {
        overlay = packed;
    }

    public int getOverlayCoords() {
        return overlay;
    }

    public static int withEmission(int packed, int emission) {
        if (emission <= 0) return packed;
        int block = Math.max(LightTexture.block(packed), Math.min(emission, 15));
        return LightTexture.pack(block, LightTexture.sky(packed));
    }

    public static int multiply(int a, int b) {
        if (a == -1) return b;
        if (b == -1) return a;
        int alpha = (a >>> 24) * (b >>> 24) / 255;
        int red = (a >> 16 & 0xFF) * (b >> 16 & 0xFF) / 255;
        int green = (a >> 8 & 0xFF) * (b >> 8 & 0xFF) / 255;
        int blue = (a & 0xFF) * (b & 0xFF) / 255;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private int colorFor(BakedQuad quad, int v) {
        return bakedColorsApplied ? colors[v] : multiply(colors[v], quad.bakedColors().color(v));
    }

    private int lightFor(BakedQuad quad, int v) {
        return withEmission(light[v], quad.materialInfo().lightEmission());
    }

    /** 26.x VertexConsumer#putBakedQuad(pose, quad, instance). */
    public static void putBakedQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Vector3f p = new Vector3f();
        Vector3f n = new Vector3f();
        int faceNormal = 0;
        for (int v = 0; v < 4; v++) {
            Vector3fc src = quad.position(v);
            matrix.transformPosition(src.x(), src.y(), src.z(), p);
            int packed = quad.bakedNormals().normal(v);
            if (BakedNormals.isUnspecified(packed)) {
                if (faceNormal == 0)
                    faceNormal = BakedNormals.computeQuadNormal(quad.position0(), quad.position1(), quad.position2(), quad.position3());
                packed = faceNormal;
            }
            BakedNormals.unpack(packed, n);
            normalMatrix.transform(n);
            long uv = quad.packedUV(v);
            consumer.addVertex(p.x(), p.y(), p.z(), instance.colorFor(quad, v), Uv.unpackU(uv), Uv.unpackV(uv),
                    instance.overlay, instance.lightFor(quad, v), n.x(), n.y(), n.z());
        }
    }

    /** 26.x VertexConsumer#putBlockBakedQuad(x, y, z, quad, instance): block-space, offset only. */
    public static void putBlockBakedQuad(VertexConsumer consumer, float x, float y, float z, BakedQuad quad, QuadInstance instance) {
        Vector3f n = new Vector3f();
        int faceNormal = 0;
        for (int v = 0; v < 4; v++) {
            Vector3fc src = quad.position(v);
            int packed = quad.bakedNormals().normal(v);
            if (BakedNormals.isUnspecified(packed)) {
                if (faceNormal == 0)
                    faceNormal = BakedNormals.computeQuadNormal(quad.position0(), quad.position1(), quad.position2(), quad.position3());
                packed = faceNormal;
            }
            BakedNormals.unpack(packed, n);
            long uv = quad.packedUV(v);
            consumer.addVertex(src.x() + x, src.y() + y, src.z() + z, instance.colorFor(quad, v), Uv.unpackU(uv), Uv.unpackV(uv),
                    instance.overlay, instance.lightFor(quad, v), n.x(), n.y(), n.z());
        }
    }
}
