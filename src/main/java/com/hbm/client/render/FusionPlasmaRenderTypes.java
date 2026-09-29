// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.rendertype.PrimitiveTopology;
import com.hbm.backport.client.rendertype.BlendFunction;
import com.hbm.backport.client.rendertype.ColorTargetState;
import com.hbm.backport.client.rendertype.DepthStencilState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.function.Function;
import com.hbm.backport.client.rendertype.BindGroupLayouts;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

public final class FusionPlasmaRenderTypes {

    public static final RenderPipeline PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_fusion_plasma")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false))
                            .withCull(false));

    private static final Function<ResourceLocation, RenderType> PLASMA =
            Util.memoize(
                    texture ->
                            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                    "ntm_fusion_plasma",
                                    RenderSetup.builder(PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .createRenderSetup()));
    private static final ResourceLocation WHITE = RenderTextures.WHITE;

    public static final RenderType UNTEXTURED_ADDITIVE =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_fusion_jet",
                    RenderSetup.builder(PIPELINE)
                            .withTexture("Sampler0", WHITE)
                            .createRenderSetup());
    static final RenderPipeline OPAQUE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                            .withLocation("pipeline/ntm_fusion_plasma_cold")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("NO_OVERLAY")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(true));
    public static final RenderType UNTEXTURED_OPAQUE =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_fusion_plasma_cold",
                    RenderSetup.builder(OPAQUE_PIPELINE)
                            .withTexture("Sampler0", WHITE)
                            .useLightmap()
                            .createRenderSetup());

    static final RenderPipeline BASE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_fusion_plasma_base")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(false));
    private static final Function<ResourceLocation, RenderType> PLASMA_BASE =
            Util.memoize(
                    texture ->
                            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                    "ntm_fusion_plasma_base",
                                    RenderSetup.builder(BASE_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .createRenderSetup()));

    static final RenderPipeline BEAM_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_fusion_plasma_beam")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false))
                            .withCull(false));
    private static final Function<ResourceLocation, RenderType> BEAM =
            Util.memoize(
                    texture ->
                            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                    "ntm_fusion_plasma_beam",
                                    RenderSetup.builder(BEAM_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .useLightmap()
                                            .createRenderSetup()));

    private FusionPlasmaRenderTypes() {}

    public static RenderType plasma(ResourceLocation texture) {
        return PLASMA.apply(texture);
    }

    public static RenderType plasmaBase(ResourceLocation texture) {
        return PLASMA_BASE.apply(texture);
    }

    public static RenderType beam(ResourceLocation texture) {
        return BEAM.apply(texture);
    }
}
