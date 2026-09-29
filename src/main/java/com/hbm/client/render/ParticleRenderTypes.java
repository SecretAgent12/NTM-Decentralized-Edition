// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.lib.Library;
import com.hbm.backport.client.rendertype.PrimitiveTopology;
import com.hbm.backport.client.rendertype.BlendFunction;
import com.hbm.backport.client.rendertype.ColorTargetState;
import com.hbm.backport.client.rendertype.DepthStencilState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.hbm.backport.client.rendertype.BlendFactor;
import com.hbm.backport.client.rendertype.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.HashMap;
import java.util.Map;
import com.hbm.backport.client.rendertype.BindGroupLayouts;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.rendertype.LayeringTransform;
import com.hbm.backport.client.rendertype.OutputTarget;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class ParticleRenderTypes {

    public static final ResourceLocation ENTITY_NO_FOG = Library.id("core/entity_nofog");

    public static final ResourceLocation PARTICLE_NO_FOG = Library.id("core/particle_nofog");

    public static final ResourceLocation LIGHTNING_NO_FOG = Library.id("core/rendertype_lightning_nofog");

    public static final ResourceLocation LINES_NO_FOG = Library.id("core/rendertype_lines_nofog");

    public static final ResourceLocation ENTITY_FADE = Library.id("core/entity_fade");
    public static final ResourceLocation PARTICLE_FADE = Library.id("core/particle_fade");
    public static final ResourceLocation PARTICLE_CUTOUT = Library.id("core/particle_cutout");
    public static final ResourceLocation TEXT_FADE = Library.id("core/text_fade");

    public static final RenderPipeline.Snippet MATRICES_NO_FOG_SNIPPET =
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .buildSnippet();

    public static final RenderPipeline.Snippet PARTICLE_NO_FOG_SNIPPET =
            RenderPipeline.builder(MATRICES_NO_FOG_SNIPPET)
                    .withVertexShader(PARTICLE_NO_FOG)
                    .withFragmentShader(PARTICLE_NO_FOG)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                    .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .buildSnippet();

    public static final RenderPipeline.Snippet LINES_NO_FOG_SNIPPET =
            RenderPipeline.builder(MATRICES_NO_FOG_SNIPPET)
                    .withVertexShader(LINES_NO_FOG)
                    .withFragmentShader(LINES_NO_FOG)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL)
                    .withPrimitiveTopology(PrimitiveTopology.LINES)
                    .withDepthStencilState(DepthStencilState.DEFAULT)
                    .buildSnippet();

    public static final RenderPipeline FLASH_NO_FOG_PIPELINE =
            flashNoFogPipeline("pipeline/ntm_particle_flash_no_fog", false);
    public static final RenderPipeline FLASH_NO_FOG_CULL_PIPELINE =
            flashNoFogPipeline("pipeline/ntm_particle_flash_no_fog_cull", true);

    public static final RenderPipeline AMAT_FLASH_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_particle_amat_flash")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ENTITY_FADE)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderType AMAT_FLASH =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_particle_amat_flash",
                    RenderSetup.builder(AMAT_FLASH_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());

    public static final RenderPipeline OPAQUE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_particle_opaque")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT));

    public static final RenderPipeline LIT_TRANSLUCENT_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_particle_lit_translucent")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA)))
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline RIFT_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_particle_rift")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withColorTargetState(new ColorTargetState(BlendFunction.INVERT))
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false))
                            .withCull(true));

    private static final float ALPHA_DISCARD_ONLY_ZERO = 0.5F / 255.0F;

    public static final RenderPipeline SKELETON_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                            .withLocation("pipeline/ntm_particle_skeleton")
                            .withShaderDefine("ALPHA_CUTOUT", ALPHA_DISCARD_ONLY_ZERO)
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withCull(true));

    public static final RenderPipeline LINES_NO_DEPTH_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/ntm_particle_lines_no_depth")
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA)))
                            .withDepthStencilState(
                                    new DepthStencilState(CompareOp.ALWAYS_PASS, false)));

    public static final RenderType LINES_NO_DEPTH =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_particle_lines_no_depth",
                    RenderSetup.builder(LINES_NO_DEPTH_PIPELINE)
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                            .createRenderSetup());

    public static final ResourceLocation WHITE = RenderTextures.WHITE;

    public static final RenderType RIFT =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_particle_rift",
                    RenderSetup.builder(RIFT_PIPELINE)
                            .withTexture("Sampler0", WHITE)
                            .createRenderSetup());

    private static final Map<ResourceLocation, RenderType> FLASH_NO_FOG_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> FLASH_NO_FOG_CULL_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> OPAQUE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> LIT_TRANSLUCENT_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> SKELETON_CACHE = new HashMap<>();

    private ParticleRenderTypes() {}

    public static RenderType opaque(ResourceLocation texture) {
        return OPAQUE_CACHE.computeIfAbsent(
                texture,
                tex ->
                        com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                "ntm_particle_opaque",
                                RenderSetup.builder(OPAQUE_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .createRenderSetup()));
    }

    public static RenderType litTranslucent(ResourceLocation texture) {
        return LIT_TRANSLUCENT_CACHE.computeIfAbsent(
                texture,
                tex ->
                        com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                "ntm_particle_lit_translucent",
                                RenderSetup.builder(LIT_TRANSLUCENT_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }

    public static RenderType skeleton(ResourceLocation texture) {
        return SKELETON_CACHE.computeIfAbsent(
                texture,
                tex ->
                        com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                "ntm_particle_skeleton",
                                RenderSetup.builder(SKELETON_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .useOverlay()
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }

    public static RenderType flashNoFogCulled(ResourceLocation texture) {
        return FLASH_NO_FOG_CULL_CACHE.computeIfAbsent(
                texture,
                tex ->
                        com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                "ntm_particle_flash_no_fog_cull",
                                RenderSetup.builder(FLASH_NO_FOG_CULL_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }

    private static RenderPipeline flashNoFogPipeline(String location, boolean cull) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(MATRICES_NO_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader(ENTITY_NO_FOG)
                        .withFragmentShader(ENTITY_NO_FOG)
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                        .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(
                                new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                        .withCull(cull));
    }

    public static RenderType flashNoFog(ResourceLocation texture) {
        return FLASH_NO_FOG_CACHE.computeIfAbsent(
                texture,
                tex ->
                        com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                "ntm_particle_flash_no_fog",
                                RenderSetup.builder(FLASH_NO_FOG_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }
}
