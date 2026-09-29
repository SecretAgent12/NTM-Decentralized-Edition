// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import com.hbm.backport.client.rendertype.RenderPipeline;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.GameRenderer;
import org.jspecify.annotations.Nullable;

/**
 * The GL state a 26.x GUI {@link RenderPipeline} stands for around the immediate 1.21.1 draws of
 * {@link GuiGraphicsExtractor} / {@code GuiRenderState}: position+color or position+uv+color quads
 * (from the pipeline's vertex format), the pipeline's blend function (rendertype's
 * {@code applyBlend}), no culling (26.x GUI pipelines do not cull).
 */
public final class GuiPipelines {

    public static final GuiPipelines GUI = new GuiPipelines(RenderPipelines.GUI);
    public static final GuiPipelines GUI_TEXTURED = new GuiPipelines(RenderPipelines.GUI_TEXTURED);

    public final RenderPipeline pipeline;
    public final boolean textured;

    private GuiPipelines(RenderPipeline pipeline) {
        this(pipeline, false);
    }

    private GuiPipelines(RenderPipeline pipeline, boolean forceUntextured) {
        this.pipeline = pipeline;
        VertexFormat f = pipeline.getVertexFormat();
        this.textured = !forceUntextured && f != null && f.contains(VertexFormatElement.UV0);
    }

    public static GuiPipelines of(@Nullable RenderPipeline pipeline) {
        if (pipeline == null || pipeline == RenderPipelines.GUI_TEXTURED) return GUI_TEXTURED;
        if (pipeline == RenderPipelines.GUI) return GUI;
        return new GuiPipelines(pipeline);
    }

    /** The same blend, without texture (fill with a textured pipeline). */
    public GuiPipelines untextured() {
        return textured ? new GuiPipelines(pipeline, true) : this;
    }

    public boolean isTextured() {
        return textured;
    }

    public VertexFormat format() {
        return isTextured() ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR;
    }

    /** Sets shader and blend state for one immediate draw; undo with {@link #end()}. */
    public void begin() {
        RenderSystem.setShader(
                isTextured() ? GameRenderer::getPositionTexColorShader : GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        pipeline.applyBlend();
    }

    public void end() {
        pipeline.clearBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
}
