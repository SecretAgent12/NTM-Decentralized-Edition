// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;

/**
 * 26.x {@code net.minecraft.client.renderer.RenderPipelines}: the vanilla snippets and pipelines the tree
 * builds on or passes around. backport: unverified: shader names and states are reconstructed from how the
 * tree composes them (and from 1.21.5+ vanilla); only their meaning for {@link RenderTypeFactory} and for GUI
 * blending ({@link RenderPipeline#applyBlend()}) matters in 1.21.1.
 */
public final class RenderPipelines {
    private static final DepthStencilState GUI_DEPTH =
            // backport: 1.21.1 GUI render types depth-test LEQUAL and write depth (item z layering)
            new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);

    public static final RenderPipeline.Snippet GLOBALS_SNIPPET =
            RenderPipeline.builder().withBindGroupLayout(BindGroupLayouts.GLOBALS).buildSnippet();
    public static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET =
            RenderPipeline.builder(GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .buildSnippet();
    public static final RenderPipeline.Snippet MATRICES_FOG_SNIPPET =
            RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.FOG)
                    .buildSnippet();
    public static final RenderPipeline.Snippet MATRICES_FOG_LIGHT_DIR_SNIPPET =
            RenderPipeline.builder(MATRICES_FOG_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.LIGHT_DIRECTIONS)
                    .buildSnippet();

    public static final RenderPipeline.Snippet ENTITY_SNIPPET =
            RenderPipeline.builder(MATRICES_FOG_LIGHT_DIR_SNIPPET)
                    .withVertexShader("core/entity")
                    .withFragmentShader("core/entity")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                    .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .buildSnippet();
    public static final RenderPipeline.Snippet LINES_SNIPPET =
            RenderPipeline.builder(MATRICES_FOG_SNIPPET)
                    .withVertexShader("core/rendertype_lines")
                    .withFragmentShader("core/rendertype_lines")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    // backport: 26.x POSITION_COLOR_NORMAL_LINE_WIDTH; 1.21.1 takes the width from RenderSystem
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                    .withPrimitiveTopology(PrimitiveTopology.LINES)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .buildSnippet();
    public static final RenderPipeline.Snippet PARTICLE_SNIPPET =
            RenderPipeline.builder(MATRICES_FOG_SNIPPET)
                    .withVertexShader("core/particle")
                    .withFragmentShader("core/particle")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                    .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .buildSnippet();
    public static final RenderPipeline.Snippet WORLD_TEXT_SNIPPET =
            RenderPipeline.builder(MATRICES_FOG_SNIPPET)
                    .withVertexShader("core/text")
                    .withFragmentShader("core/text")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .buildSnippet();
    public static final RenderPipeline.Snippet GUI_SNIPPET =
            RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                    .withVertexShader("core/gui")
                    .withFragmentShader("core/gui")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(GUI_DEPTH)
                    .withCull(false)
                    .buildSnippet();
    public static final RenderPipeline.Snippet GUI_TEXTURED_SNIPPET =
            RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(GUI_DEPTH)
                    .withCull(false)
                    .buildSnippet();

    public static final RenderPipeline GUI =
            RenderPipeline.builder(GUI_SNIPPET).withLocation("pipeline/gui").build();
    public static final RenderPipeline GUI_TEXTURED =
            RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_textured").build();
    public static final RenderPipeline GUI_TEXTURED_PREMULTIPLIED_ALPHA =
            RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/gui_textured_premultiplied_alpha")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
                    .build();
    public static final RenderPipeline GUI_INVERT =
            RenderPipeline.builder(GUI_SNIPPET)
                    .withLocation("pipeline/gui_invert")
                    .withColorTargetState(new ColorTargetState(BlendFunction.INVERT))
                    .build();
    public static final RenderPipeline CROSSHAIR =
            RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/crosshair")
                    .withColorTargetState(new ColorTargetState(BlendFunction.INVERT))
                    .build();

    public static final RenderPipeline TEXT =
            RenderPipeline.builder(WORLD_TEXT_SNIPPET).withLocation("pipeline/text").build();
    public static final RenderPipeline TEXT_GRAYSCALE =
            RenderPipeline.builder(WORLD_TEXT_SNIPPET)
                    .withLocation("pipeline/text_grayscale")
                    .withShaderDefine("IS_GRAYSCALE")
                    .build();

    public static final RenderPipeline LINES =
            RenderPipeline.builder(LINES_SNIPPET).withLocation("pipeline/lines").build();

    public static final RenderPipeline ENTITY_CUTOUT =
            RenderPipeline.builder(ENTITY_SNIPPET)
                    .withLocation("pipeline/entity_cutout")
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withCull(false)
                    .build();

    public static final RenderPipeline CRUMBLING =
            RenderPipeline.builder(MATRICES_FOG_SNIPPET)
                    .withLocation("pipeline/crumbling")
                    .withVertexShader("core/rendertype_crumbling")
                    .withFragmentShader("core/rendertype_crumbling")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withColorTargetState(new ColorTargetState(BlendFunction.CRUMBLING))
                    .withVertexBinding(0, DefaultVertexFormat.BLOCK)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false, -1F, -10F))
                    .build();

    public static final RenderPipeline CELESTIAL =
            RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/celestial")
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(null)
                    .build();

    private RenderPipelines() {}

    /** backport: helper for pipelines named in the mod namespace. */
    static ResourceLocation hbm(String path) {
        return ResourceLocation.fromNamespaceAndPath("hbm", path);
    }
}
