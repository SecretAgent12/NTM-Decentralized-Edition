// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.rendertype.BlendFunction;
import com.hbm.backport.client.rendertype.ColorTargetState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import com.hbm.backport.client.core.ImmediateSubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypeInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public final class TextRenderTypes {

    public static final RenderPipeline ADDITIVE_PIPELINE =
            pipeline("pipeline/ntm_text_additive", false);
    public static final RenderPipeline ADDITIVE_GRAYSCALE_PIPELINE =
            pipeline("pipeline/ntm_text_additive_grayscale", true);
    private static final Map<ResourceLocation, RenderType> ADDITIVE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RenderType> ADDITIVE_GRAYSCALE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private TextRenderTypes() {}

    public static RenderType additive(ResourceLocation sheet) {
        return ADDITIVE.computeIfAbsent(
                sheet, s -> type("ntm_text_additive", ADDITIVE_PIPELINE, s));
    }

    public static RenderType additiveGrayscale(ResourceLocation sheet) {
        return ADDITIVE_GRAYSCALE.computeIfAbsent(
                sheet, s -> type("ntm_text_additive_grayscale", ADDITIVE_GRAYSCALE_PIPELINE, s));
    }

    public static void submitAdditive(
            SubmitNodeCollector collector,
            PoseStack pose,
            Font font,
            FormattedCharSequence text,
            int color,
            int lightCoords) {
        // backport: 1.21.1 Font has no prepareText/GlyphVisitor; draw the text through a buffer source
        // that swaps each glyph sheet's text type for its additive twin (26.x: TEXT -> additive,
        // TEXT_GRAYSCALE -> additiveGrayscale). The collector draws immediately in 1.21.1, so the glyphs
        // go straight into its buffers (the frame's buffer source for decorating collectors).
        MultiBufferSource buffers =
                collector instanceof ImmediateSubmitNodeCollector immediate
                        ? immediate.buffers()
                        : Minecraft.getInstance().renderBuffers().bufferSource();
        MultiBufferSource additiveBuffers =
                sheet -> {
                    ResourceLocation texture = RenderTypeInfo.texture(sheet);
                    RenderType type;
                    if (texture == null) type = sheet;
                    else if (RenderTypeInfo.isTextIntensity(sheet)) type = additiveGrayscale(texture);
                    else if (RenderTypeInfo.isText(sheet)) type = additive(texture);
                    else type = sheet;
                    return buffers.getBuffer(type);
                };
        font.drawInBatch(
                text,
                0F,
                0F,
                color,
                false,
                pose.last().pose(),
                additiveBuffers,
                Font.DisplayMode.NORMAL,
                0,
                lightCoords);
    }

    private static RenderType type(String name, RenderPipeline pipeline, ResourceLocation sheet) {
        return com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                name,
                RenderSetup.builder(pipeline)
                        .withTexture("Sampler0", sheet)
                        .useLightmap()
                        .createRenderSetup());
    }

    private static RenderPipeline pipeline(String location, boolean grayscale) {
        RenderPipeline.Builder builder =
                RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/text")
                        .withFragmentShader(ParticleRenderTypes.TEXT_FADE)
                        .withShaderDefine("ALPHA_CUTOUT", 0F)
                        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                        .withCull(false);
        if (grayscale) builder.withShaderDefine("IS_GRAYSCALE");
        return WorldRenderPipeline.of(builder);
    }
}
