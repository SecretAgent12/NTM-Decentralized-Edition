// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.state;

import com.hbm.backport.client.gui.render.TextureSetup;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.state.gui.BlitRenderState}: one textured quad. */
public record BlitRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        int x0,
        int y0,
        int x1,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds)
        implements GuiElementRenderState {

    public BlitRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            int x0,
            int y0,
            int x1,
            int y1,
            float u0,
            float u1,
            float v0,
            float v1,
            int color,
            @Nullable ScreenRectangle scissorArea) {
        this(pipeline, textureSetup, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, scissorArea, null);
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        Vector2f p = new Vector2f();
        pose.transformPosition(x0, y0, p);
        vertices.addVertex(p.x, p.y, 0F).setUv(u0, v0).setColor(color);
        pose.transformPosition(x0, y1, p);
        vertices.addVertex(p.x, p.y, 0F).setUv(u0, v1).setColor(color);
        pose.transformPosition(x1, y1, p);
        vertices.addVertex(p.x, p.y, 0F).setUv(u1, v1).setColor(color);
        pose.transformPosition(x1, y0, p);
        vertices.addVertex(p.x, p.y, 0F).setUv(u1, v0).setColor(color);
    }
}
