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
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.hbm.backport.client.rendertype.BindGroupLayouts;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class TorexRenderTypes {

    public static final RenderPipeline FLARE_PIPELINE =
            billboard("pipeline/ntm_torex_flare", BlendFunction.LIGHTNING, false);
    public static final RenderPipeline FLASH_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(ParticleRenderTypes.MATRICES_NO_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_torex_flash")
                            .withVertexShader(ParticleRenderTypes.LIGHTNING_NO_FOG)
                            .withFragmentShader(ParticleRenderTypes.LIGHTNING_NO_FOG)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT));
    public static final RenderType FLASH =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_torex_flash", RenderSetup.builder(FLASH_PIPELINE).createRenderSetup());
    private static final ResourceLocation CLOUDLET_TEX =
            Library.id("textures/particle/particle_base.png");
    private static final ResourceLocation FLARE_TEX = Library.id("textures/particle/flare.png");
    public static final RenderType FLARE =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_torex_flare",
                    RenderSetup.builder(FLARE_PIPELINE)
                            .withTexture("Sampler0", FLARE_TEX)
                            .useLightmap()
                            .createRenderSetup());
    private static final BlendFunction CLOUDLET_BLEND =
            new BlendFunction(
                    BlendFactor.SRC_ALPHA,
                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                    BlendFactor.ONE,
                    BlendFactor.ZERO);
    public static final RenderPipeline CLOUDLET_PIPELINE =
            billboard("pipeline/ntm_torex_cloudlet", CLOUDLET_BLEND, false);
    public static final RenderType CLOUDLET =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_torex_cloudlet",
                    RenderSetup.builder(CLOUDLET_PIPELINE)
                            .withTexture("Sampler0", CLOUDLET_TEX)
                            .useLightmap()
                            .createRenderSetup());

    private TorexRenderTypes() {}

    private static RenderPipeline billboard(String location, BlendFunction blend, boolean cull) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(ParticleRenderTypes.MATRICES_NO_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader(ParticleRenderTypes.ENTITY_NO_FOG)
                        .withFragmentShader(ParticleRenderTypes.ENTITY_NO_FOG)
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withColorTargetState(new ColorTargetState(blend))
                        .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(DepthStencilState.DEFAULT)
                        .withCull(cull));
    }
}
