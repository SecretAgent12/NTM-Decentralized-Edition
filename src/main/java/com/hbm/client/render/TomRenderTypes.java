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

public final class TomRenderTypes {

    public static final RenderPipeline FLAME_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_tom_flame")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(false));

    private static final Function<ResourceLocation, RenderType> FLAME =
            Util.memoize(
                    texture ->
                            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                                    "ntm_tom_flame",
                                    RenderSetup.builder(FLAME_PIPELINE)
                                            .withTexture("Sampler0", texture)
                                            .useLightmap()
                                            .createRenderSetup()));

    private TomRenderTypes() {}

    public static RenderType flame(ResourceLocation texture) {
        return FLAME.apply(texture);
    }
}
