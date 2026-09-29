// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.client.render.WorldRenderPipeline;
import com.hbm.backport.client.rendertype.BlendFunction;
import com.hbm.backport.client.rendertype.ColorTargetState;
import com.hbm.backport.client.rendertype.DepthStencilState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.hbm.backport.client.rendertype.BlendFactor;
import com.hbm.backport.client.particle.SingleQuadParticle;
import com.hbm.backport.client.rendertype.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.texture.TextureAtlas;

public final class ParticleLayers {
    private static final float EFFECT_RENDERER_ALPHA_CUTOUT = 1.5F / 255.0F;

    public static final RenderPipeline TRANSLUCENT_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                            .withLocation("pipeline/ntm_particle_translucent")
                            .withFragmentShader(ParticleRenderTypes.PARTICLE_CUTOUT)
                            .withShaderDefine("ALPHA_CUTOUT", EFFECT_RENDERER_ALPHA_CUTOUT)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA)))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline TRANSLUCENT_SEPARATE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                            .withLocation("pipeline/ntm_particle_translucent_separate")
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline ADDITIVE_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                            .withLocation("pipeline/ntm_particle_additive")
                            .withShaderDefine("ALPHA_CUTOUT", 0F)
                            .withFragmentShader(ParticleRenderTypes.PARTICLE_FADE)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline ADDITIVE_NO_FOG_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(ParticleRenderTypes.PARTICLE_NO_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_particle_additive_no_fog")
                            .withShaderDefine("ALPHA_CUTOUT", 0F)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    // backport: a 1.21.1 layer carries the RenderType its quads are drawn with; 26.x draws the layer's
    // pipeline with the particle atlas as Sampler0 and the lightmap, which is this RenderSetup
    public static final SingleQuadParticle.Layer TRANSLUCENT =
            layer("ntm_particle_translucent", TRANSLUCENT_PIPELINE);
    public static final SingleQuadParticle.Layer TRANSLUCENT_SEPARATE =
            layer("ntm_particle_translucent_separate", TRANSLUCENT_SEPARATE_PIPELINE);
    public static final SingleQuadParticle.Layer ADDITIVE =
            layer("ntm_particle_additive", ADDITIVE_PIPELINE);
    public static final SingleQuadParticle.Layer ADDITIVE_NO_FOG =
            layer("ntm_particle_additive_no_fog", ADDITIVE_NO_FOG_PIPELINE);

    private static SingleQuadParticle.Layer layer(String name, RenderPipeline pipeline) {
        return new SingleQuadParticle.Layer(
                true,
                TextureAtlas.LOCATION_PARTICLES,
                com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                        name,
                        RenderSetup.builder(pipeline)
                                .withTexture("Sampler0", TextureAtlas.LOCATION_PARTICLES)
                                .useLightmap()
                                .createRenderSetup()));
    }

    private ParticleLayers() {}
}
