// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.blockmodel.BakedQuad;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.SubmitNodeCollection} (one order bucket of a {@link
 * SubmitNodeStorage}). backport: 1.21.1 renders immediately and never replays stored nodes, so this
 * is a sink that discards what is submitted; the tree only uses it as a base for its own collectors.
 */
public class SubmitNodeCollection implements OrderedSubmitNodeCollector {
    @Override
    public void submitCustomGeometry(
            PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer r) {}

    @Override
    public void submitText(
            PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow,
            Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor,
            int outlineColor) {}

    @Override
    public void submitNameTag(
            PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name,
            boolean seeThrough, int lightCoords, CameraRenderState camera) {}

    @Override
    public void submitItem(
            PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords,
            int outlineColor, int[] tintLayers, List<BakedQuad> quads,
            ItemStackRenderState.FoilType foilType) {}
}
