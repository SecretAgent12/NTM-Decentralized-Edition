// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.state;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.gui.GuiPipelines;
import com.hbm.backport.client.gui.render.TextureSetup;
import com.hbm.backport.client.gui.render.pip.PictureInPictureRenderers;
import com.hbm.backport.client.gui.state.pip.PictureInPictureRenderState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix4fStack;

/**
 * 26.x {@code net.minecraft.client.renderer.state.gui.GuiRenderState} as reached through {@code
 * graphics.guiRenderState}: 26.x collects the elements and draws them after extraction; here each
 * one is drawn when it is added, in the order the screen adds it (which is what 26.x's layering
 * resolves to for the tree's non-overlapping gauges and panels).
 */
public final class GuiRenderState {

    private final GuiGraphicsExtractor graphics;

    public GuiRenderState(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    public void addGuiElement(GuiElementRenderState element) {
        draw(graphics, element);
    }

    public void submitGuiElement(GuiElementRenderState element) {
        draw(graphics, element);
    }

    public void addBlitToCurrentLayer(BlitRenderState blit) {
        draw(graphics, blit);
    }

    public void addPicturesInPictureState(PictureInPictureRenderState state) {
        PictureInPictureRenderers.render(graphics, state);
    }

    public void submitPicturesInPictureState(PictureInPictureRenderState state) {
        addPicturesInPictureState(state);
    }

    /** Draws one element now: its pipeline's format/blend, its texture, its scissor. */
    public static void draw(GuiGraphicsExtractor graphics, GuiElementRenderState element) {
        graphics.flush();
        GuiPipelines p = GuiPipelines.of(element.pipeline());
        ScreenRectangle scissor = element.scissorArea();
        if (scissor != null) graphics.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());
        TextureSetup texture = element.textureSetup();
        if (p.textured && texture != null && !texture.isEmpty()) RenderSystem.setShaderTexture(0, texture.glId());
        p.begin();
        // the element's vertices are already posed in 2D; keep the 1.21.1 GUI z of the current pose
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.translate(0F, 0F, graphics.pose().z());
        RenderSystem.applyModelViewMatrix();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, p.format());
        element.buildVertices(b);
        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
        p.end();
        if (scissor != null) graphics.disableScissor();
    }
}
