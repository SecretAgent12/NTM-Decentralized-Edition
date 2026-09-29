// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.render.pip;

import com.hbm.backport.client.core.ImmediateSubmitNodeCollector;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.gui.GuiPipelines;
import com.hbm.backport.client.gui.state.GuiRenderState;
import com.hbm.backport.client.gui.state.pip.PictureInPictureRenderState;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.gui.render.pip.PictureInPictureRenderer} on 1.21.1: {@link
 * #prepare} renders the state's 3D content into an offscreen {@link TextureTarget} the size of its
 * rectangle in real pixels (orthographic, y down, origin at the rectangle's centre / {@link
 * #getTranslateY}, {@code guiScale * scale()} pixels per unit, z flipped), then draws that texture
 * into the GUI at the rectangle, as 26.x does. Nodes submitted in {@link #renderToTexture} are drawn
 * through core's ImmediateSubmitNodeCollector into this renderer's buffer source.
 */
public abstract class PictureInPictureRenderer<T extends PictureInPictureRenderState> implements AutoCloseable {

    protected final MultiBufferSource.BufferSource bufferSource;
    /** The offscreen colour+depth target of the last {@link #prepare} (26.x: the GpuTexture). */
    protected @Nullable TextureTarget texture;

    protected PictureInPictureRenderer() {
        this(Minecraft.getInstance().renderBuffers().bufferSource());
    }

    protected PictureInPictureRenderer(MultiBufferSource.BufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    public abstract Class<T> getRenderStateClass();

    protected abstract void renderToTexture(T state, PoseStack pose, SubmitNodeCollector collector);

    protected abstract String getTextureLabel();

    protected float getTranslateY(int height, int guiScale) {
        return height / 2F;
    }

    public void prepare(T state, GuiRenderState gui, FeatureRenderDispatcher features, int guiScale) {
        int width = (state.x1() - state.x0()) * guiScale;
        int height = (state.y1() - state.y0()) * guiScale;
        if (width <= 0 || height <= 0) return;
        if (texture == null || texture.width != width || texture.height != height) {
            if (texture != null) texture.destroyBuffers();
            texture = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        }
        texture.setClearColor(0F, 0F, 0F, 0F);
        texture.clear(Minecraft.ON_OSX);
        texture.bindWrite(true);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0F, width, height, 0F, -1000F, 1000F), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();

        PoseStack pose = new PoseStack();
        pose.translate(width / 2F, getTranslateY(height, guiScale), 0F);
        float scale = guiScale * state.scale();
        pose.scale(scale, scale, -scale);
        renderToTexture(state, pose, new ImmediateSubmitNodeCollector(bufferSource));
        bufferSource.endBatch();

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(true);
        Lighting.setupFor3DItems();
    }

    /** Draws the prepared texture into the GUI at the state's rectangle (v flipped: GL rows are bottom-up). */
    protected void blitTexture(com.hbm.backport.client.gui.GuiGraphicsExtractor graphics, T state) {
        if (texture == null) return;
        graphics.flush();
        var scissor = state.scissorArea();
        if (scissor != null) graphics.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());
        RenderSystem.setShaderTexture(0, texture.getColorTextureId());
        GuiPipelines p = GuiPipelines.GUI_TEXTURED;
        p.begin();
        Matrix4f m = graphics.pose().last().pose();
        var b = com.mojang.blaze3d.vertex.Tesselator.getInstance()
                .begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, p.format());
        b.addVertex(m, state.x0(), state.y0(), 0F).setUv(0F, 1F).setColor(-1);
        b.addVertex(m, state.x0(), state.y1(), 0F).setUv(0F, 0F).setColor(-1);
        b.addVertex(m, state.x1(), state.y1(), 0F).setUv(1F, 0F).setColor(-1);
        b.addVertex(m, state.x1(), state.y0(), 0F).setUv(1F, 1F).setColor(-1);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(b.buildOrThrow());
        p.end();
        if (scissor != null) graphics.disableScissor();
    }

    /** prepare + blit, what 26.x's GuiRenderer does for each submitted state of this renderer. */
    public void render(com.hbm.backport.client.gui.GuiGraphicsExtractor graphics, T state) {
        int guiScale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        graphics.flush();
        prepare(state, graphics.guiRenderState, FeatureRenderDispatcher.IMMEDIATE, guiScale);
        blitTexture(graphics, state);
    }

    @Override
    public void close() {
        if (texture != null) {
            texture.destroyBuffers();
            texture = null;
        }
    }
}
