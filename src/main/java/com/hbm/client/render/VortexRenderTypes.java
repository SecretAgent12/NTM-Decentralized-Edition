// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.rendertype.PrimitiveTopology;
import com.hbm.backport.client.rendertype.BlendFunction;
import com.hbm.backport.client.rendertype.ColorTargetState;
import com.hbm.backport.client.rendertype.DepthStencilState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.hbm.backport.client.rendertype.BlendFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.function.Function;
import com.hbm.backport.client.rendertype.BindGroupLayouts;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

public final class VortexRenderTypes {

    public static final RenderPipeline TRANSLUCENT_PIPELINE =
            pipeline(
                    "pipeline/ntm_vortex_translucent",
                    new BlendFunction(
                            BlendFactor.SRC_ALPHA,
                            BlendFactor.ONE_MINUS_SRC_ALPHA,
                            BlendFactor.ONE,
                            BlendFactor.ZERO));
    public static final RenderPipeline ADDITIVE_PIPELINE =
            pipeline("pipeline/ntm_vortex_additive", BlendFunction.LIGHTNING);
    public static final RenderPipeline CUTOUT_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_vortex_cutout")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(false));

    private static final Function<ResourceLocation, RenderType> TRANSLUCENT =
            Util.memoize(
                    texture ->
                            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                    "ntm_vortex_translucent",
                                    RenderSetup.builder(TRANSLUCENT_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .sortOnUpload()
                                            .createRenderSetup()));
    private static final Function<ResourceLocation, RenderType> ADDITIVE =
            Util.memoize(
                    texture ->
                            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                    "ntm_vortex_additive",
                                    RenderSetup.builder(ADDITIVE_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .sortOnUpload()
                                            .createRenderSetup()));
    private static final Function<ResourceLocation, RenderType> CUTOUT =
            Util.memoize(
                    texture ->
                            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                    "ntm_vortex_cutout",
                                    RenderSetup.builder(CUTOUT_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .createRenderSetup()));

    private VortexRenderTypes() {}

    private static RenderPipeline pipeline(String location, BlendFunction blend) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader(
                                blend == BlendFunction.LIGHTNING
                                        ? ParticleRenderTypes.ENTITY_FADE
                                        : ResourceLocation.withDefaultNamespace("core/entity"))
                        .withShaderDefine("EMISSIVE")
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                        .withColorTargetState(new ColorTargetState(blend))
                        .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(
                                new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                        .withCull(false));
    }

    public static RenderType translucent(ResourceLocation texture) {
        return TRANSLUCENT.apply(texture);
    }

    public static RenderType additive(ResourceLocation texture) {
        return ADDITIVE.apply(texture);
    }

    public static RenderType cutout(ResourceLocation texture) {
        return CUTOUT.apply(texture);
    }
}
