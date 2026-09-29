// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.state;

import com.hbm.backport.client.gui.render.TextureSetup;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.state.gui.GuiElementRenderState}: a GUI element that
 * writes its own (already posed) quads. {@link GuiRenderState#addGuiElement} draws it immediately.
 */
public interface GuiElementRenderState extends ScreenArea {

    void buildVertices(VertexConsumer vertices);

    RenderPipeline pipeline();

    TextureSetup textureSetup();

    @Nullable ScreenRectangle scissorArea();
}
