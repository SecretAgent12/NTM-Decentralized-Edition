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
import org.jspecify.annotations.Nullable;

public final class FlatTranslucent {

    private static final float ALPHA_DISCARD_ONLY_ZERO = 0.5F / 255.0F;
    private static final BlendFunction BLEND =
            new BlendFunction(
                    BlendFactor.SRC_ALPHA,
                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                    BlendFactor.ONE,
                    BlendFactor.ZERO);

    static final RenderPipeline NO_CULL = pipeline("pipeline/ntm_flat_translucent", false, false);
    static final RenderPipeline CULL = pipeline("pipeline/ntm_flat_translucent_cull", true, false);
    static final RenderPipeline DEPTH_WRITE =
            pipeline("pipeline/ntm_flat_translucent_depth_write", false, true);
    static final RenderPipeline LIT_NO_CULL =
            litPipeline("pipeline/ntm_lit_translucent", false, BLEND, ALPHA_DISCARD_ONLY_ZERO);
    static final RenderPipeline LIT_CULL =
            litPipeline("pipeline/ntm_lit_translucent_cull", true, BLEND, 0.1F);
    static final RenderPipeline LIT_PLAIN =
            litPipeline(
                    "pipeline/ntm_lit_translucent_plain",
                    false,
                    new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA),
                    0F);
    static final RenderPipeline LIT_CUTOUT =
            litPipeline("pipeline/ntm_lit_cutout_no_depth", false, null, 0.1F);

    private static final Function<ResourceLocation, RenderType> NO_CULL_TYPES =
            Util.memoize(texture -> type("ntm_flat_translucent", NO_CULL, texture));
    private static final Function<ResourceLocation, RenderType> CULL_TYPES =
            Util.memoize(texture -> type("ntm_flat_translucent_cull", CULL, texture));
    private static final Function<ResourceLocation, RenderType> DEPTH_WRITE_TYPES =
            Util.memoize(texture -> type("ntm_flat_translucent_depth_write", DEPTH_WRITE, texture));
    private static final Function<ResourceLocation, RenderType> LIT_TYPES =
            Util.memoize(texture -> type("ntm_lit_translucent", LIT_NO_CULL, texture));
    private static final Function<ResourceLocation, RenderType> LIT_CULL_TYPES =
            Util.memoize(texture -> type("ntm_lit_translucent_cull", LIT_CULL, texture));
    private static final Function<ResourceLocation, RenderType> LIT_PLAIN_TYPES =
            Util.memoize(texture -> type("ntm_lit_translucent_plain", LIT_PLAIN, texture));
    private static final Function<ResourceLocation, RenderType> LIT_CUTOUT_TYPES =
            Util.memoize(texture -> type("ntm_lit_cutout_no_depth", LIT_CUTOUT, texture));

    private FlatTranslucent() {}

    private static RenderPipeline pipeline(String location, boolean cull, boolean writeDepth) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader("core/entity")
                        .withShaderDefine("ALPHA_CUTOUT", ALPHA_DISCARD_ONLY_ZERO)
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withColorTargetState(new ColorTargetState(BLEND))
                        .withDepthStencilState(
                                new DepthStencilState(
                                        DepthStencilState.DEFAULT.depthTest(), writeDepth))
                        .withCull(cull));
    }

    private static RenderPipeline litPipeline(
            String location, boolean cull, @Nullable BlendFunction blend, float alphaCutout) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader("core/entity")
                        .withShaderDefine("ALPHA_CUTOUT", alphaCutout)
                        .withShaderDefine("NO_OVERLAY")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withColorTargetState(
                                blend == null
                                        ? ColorTargetState.DEFAULT
                                        : new ColorTargetState(blend))
                        .withDepthStencilState(
                                new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                        .withCull(cull));
    }

    private static RenderType type(String name, RenderPipeline pipeline, ResourceLocation texture) {
        return com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                name,
                RenderSetup.builder(pipeline)
                        .withTexture("Sampler0", texture)
                        .useLightmap()
                        .affectsCrumbling()
                        .createRenderSetup());
    }

    public static RenderType of(ResourceLocation texture) {
        return NO_CULL_TYPES.apply(texture);
    }

    public static RenderType culled(ResourceLocation texture) {
        return CULL_TYPES.apply(texture);
    }

    public static RenderType depthWriting(ResourceLocation texture) {
        return DEPTH_WRITE_TYPES.apply(texture);
    }

    public static RenderType lit(ResourceLocation texture) {
        return LIT_TYPES.apply(texture);
    }

    public static RenderType litCulled(ResourceLocation texture) {
        return LIT_CULL_TYPES.apply(texture);
    }

    public static RenderType litPlain(ResourceLocation texture) {
        return LIT_PLAIN_TYPES.apply(texture);
    }

    public static RenderType litCutout(ResourceLocation texture) {
        return LIT_CUTOUT_TYPES.apply(texture);
    }
}
