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
import com.hbm.backport.client.rendertype.BindGroupLayouts;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class CloudRenderTypes {

    public static final RenderPipeline OPAQUE_PIPELINE =
            pipeline("pipeline/ntm_cloud_opaque", null, true, false);
    public static final RenderPipeline OPAQUE_UNCULLED_PIPELINE =
            pipeline("pipeline/ntm_cloud_opaque_unculled", null, false, false);
    public static final RenderPipeline TRANSLUCENT_PIPELINE =
            pipeline(
                    "pipeline/ntm_cloud_translucent",
                    new BlendFunction(
                            BlendFactor.SRC_ALPHA,
                            BlendFactor.ONE_MINUS_SRC_ALPHA,
                            BlendFactor.ONE,
                            BlendFactor.ZERO),
                    true,
                    false);
    public static final RenderPipeline ADDITIVE_PIPELINE =
            pipeline("pipeline/ntm_cloud_additive", BlendFunction.LIGHTNING, true, false);
    public static final RenderPipeline ADDITIVE_LIT_UNCULLED_PIPELINE =
            pipeline(
                    "pipeline/ntm_cloud_additive_lit_unculled",
                    BlendFunction.LIGHTNING,
                    false,
                    true);
    private static final ResourceLocation WHITE = RenderTextures.WHITE;
    public static final RenderType OPAQUE = create("ntm_cloud_opaque", OPAQUE_PIPELINE);
    public static final RenderType OPAQUE_UNCULLED =
            create("ntm_cloud_opaque_unculled", OPAQUE_UNCULLED_PIPELINE);
    public static final RenderType TRANSLUCENT =
            create("ntm_cloud_translucent", TRANSLUCENT_PIPELINE);
    public static final RenderType ADDITIVE = create("ntm_cloud_additive", ADDITIVE_PIPELINE);
    public static final RenderType ADDITIVE_LIT_UNCULLED =
            create("ntm_cloud_additive_lit_unculled", ADDITIVE_LIT_UNCULLED_PIPELINE);

    private CloudRenderTypes() {}

    private static RenderPipeline pipeline(
            String location, BlendFunction blend, boolean cull, boolean lit) {
        RenderPipeline.Builder builder =
                RenderPipeline.builder(
                        lit
                                ? RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET
                                : RenderPipelines.MATRICES_FOG_SNIPPET);
        if (!lit) builder.withShaderDefine("NO_CARDINAL_LIGHTING");
        return WorldRenderPipeline.of(
                builder.withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader(
                                blend == BlendFunction.LIGHTNING
                                        ? ParticleRenderTypes.ENTITY_FADE
                                        : ResourceLocation.withDefaultNamespace("core/entity"))
                        .withShaderDefine("EMISSIVE")
                        .withShaderDefine("NO_OVERLAY")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                        .withColorTargetState(
                                blend == null
                                        ? ColorTargetState.DEFAULT
                                        : new ColorTargetState(blend))
                        .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(DepthStencilState.DEFAULT)
                        .withCull(cull));
    }

    private static RenderType create(String name, RenderPipeline pipeline) {
        return com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                name,
                RenderSetup.builder(pipeline).withTexture("Sampler0", WHITE).createRenderSetup());
    }
}
