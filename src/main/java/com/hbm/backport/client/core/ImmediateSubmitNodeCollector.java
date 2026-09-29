// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import com.hbm.backport.client.blockmodel.BakedQuad;
import com.hbm.backport.client.itemmodel.ItemQuads;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * The collector a bridged 1.21.1 renderer hands to a 26.x-shaped {@code submit}: every node is drawn
 * right away into the frame's {@link MultiBufferSource} (which in 1.21.1 already carries the
 * outline and crumbling wrapping the dispatcher chose for this renderer).
 */
public class ImmediateSubmitNodeCollector implements SubmitNodeCollector {
    protected final MultiBufferSource buffers;

    public ImmediateSubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public MultiBufferSource buffers() {
        return buffers;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        // backport: immediate mode draws in submission order
        return this;
    }

    @Override
    public void submitCustomGeometry(
            PoseStack poseStack,
            RenderType renderType,
            SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        customGeometryRenderer.render(poseStack.last(), buffers.getBuffer(renderType));
    }

    @Override
    public void submitText(
            PoseStack poseStack,
            float x,
            float y,
            FormattedCharSequence string,
            boolean dropShadow,
            Font.DisplayMode displayMode,
            int lightCoords,
            int color,
            int backgroundColor,
            int outlineColor) {
        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = poseStack.last().pose();
        if (outlineColor != 0) {
            // 26.x outlined text (glowing sign ink) -> 1.21.1's 8x outline pass
            font.drawInBatch8xOutline(
                    string, x, y, color, outlineColor, matrix, buffers, lightCoords);
        } else {
            font.drawInBatch(
                    string,
                    x,
                    y,
                    color,
                    dropShadow,
                    matrix,
                    buffers,
                    displayMode,
                    backgroundColor,
                    lightCoords);
        }
    }

    @Override
    public void submitNameTag(
            PoseStack poseStack,
            @Nullable Vec3 nameTagAttachment,
            int offset,
            Component name,
            boolean seeThrough,
            int lightCoords,
            CameraRenderState camera) {
        if (nameTagAttachment == null) return;
        // port of 1.21.1 EntityRenderer.renderNameTag
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        poseStack.pushPose();
        poseStack.translate(nameTagAttachment.x, nameTagAttachment.y + 0.5, nameTagAttachment.z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.025F, -0.025F, 0.025F);
        Matrix4f matrix = poseStack.last().pose();
        int background = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        float x = -font.width(name) / 2F;
        font.drawInBatch(
                name,
                x,
                offset,
                0x20FFFFFF,
                false,
                matrix,
                buffers,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                background,
                lightCoords);
        if (seeThrough) {
            font.drawInBatch(
                    name, x, offset, -1, false, matrix, buffers, Font.DisplayMode.NORMAL, 0,
                    lightCoords);
        }
        poseStack.popPose();
    }

    @Override
    public void submitItem(
            PoseStack poseStack,
            ItemDisplayContext displayContext,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            int[] tintLayers,
            List<BakedQuad> quads,
            ItemStackRenderState.FoilType foilType) {
        // render type per quad (MaterialInfo), foil and tints: the item model topic's 1.21.1 drawing
        ItemQuads.render(
                buffers, poseStack.last(), displayContext, lightCoords, overlayCoords, tintLayers,
                quads, foilType);
    }
}
