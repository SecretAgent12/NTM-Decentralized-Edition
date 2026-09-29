// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.rendertype.PrimitiveTopology;
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

public final class FlatCutout {

    static final RenderPipeline PIPELINE = pipeline("pipeline/ntm_flat_cutout", false);
    static final RenderPipeline CULL_PIPELINE = pipeline("pipeline/ntm_flat_cutout_cull", true);

    private static final Function<ResourceLocation, RenderType> TYPES =
            types("ntm_flat_cutout", PIPELINE);
    private static final Function<ResourceLocation, RenderType> CULLED =
            types("ntm_flat_cutout_cull", CULL_PIPELINE);

    private FlatCutout() {}

    public static RenderType of(ResourceLocation texture) {
        return TYPES.apply(texture);
    }

    public static RenderType culled(ResourceLocation texture) {
        return CULLED.apply(texture);
    }

    private static RenderPipeline pipeline(String location, boolean cull) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader("core/entity")
                        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(DepthStencilState.DEFAULT)
                        .withCull(cull));
    }

    private static Function<ResourceLocation, RenderType> types(String name, RenderPipeline pipeline) {
        return Util.memoize(
                texture ->
                        com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                name,
                                RenderSetup.builder(pipeline)
                                        .withTexture("Sampler0", texture)
                                        .useLightmap()
                                        .affectsCrumbling()
                                        .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                                        .createRenderSetup()));
    }
}
