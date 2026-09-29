// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.particle.SingleQuadParticle}: a camera-facing textured quad that
 * extracts itself into a {@link QuadParticleRenderState} under a {@link Layer}.
 *
 * <p>backport: built on 1.21.1 {@link TextureSheetParticle} (sprite, {@code setSprite}, {@code
 * pickSprite}, {@code setSpriteFromAge}, {@code getU0..getV1}, {@code quadSize}). The engine keys it by
 * a per-{@link Layer} 1.21.1 render type ({@link #getRenderType}); that type extracts all its particles
 * through {@link #extract} and draws the resulting state (see {@link ParticleBridge}). The 1.21.1
 * per-particle {@link #render} entry point also goes through {@code extract}, drawing into the
 * given consumer.
 */
public abstract class SingleQuadParticle extends TextureSheetParticle {

    protected SingleQuadParticle(
            ClientLevel level, double x, double y, double z, @Nullable TextureAtlasSprite sprite) {
        super(level, x, y, z);
        if (sprite != null) setSprite(sprite);
    }

    protected SingleQuadParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            @Nullable TextureAtlasSprite sprite) {
        super(level, x, y, z, xd, yd, zd);
        if (sprite != null) setSprite(sprite);
    }

    // ---- 26.x API ------------------------------------------------------------------------

    protected abstract Layer getLayer();

    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    /** 26.x name of {@code getLightColor}. */
    protected int getLightCoords(float partialTicks) {
        return super.getLightColor(partialTicks);
    }

    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        Quaternionf rotation = new Quaternionf();
        this.getFacingCameraMode().setRotation(rotation, camera, partialTicks);
        if (this.roll != 0.0F) {
            rotation.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        }
        this.extractRotatedQuad(state, camera, rotation, partialTicks);
    }

    protected void extractRotatedQuad(
            QuadParticleRenderState state, Camera camera, Quaternionf rotation, float partialTicks) {
        var pos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - pos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - pos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - pos.z());
        this.extractRotatedQuad(state, rotation, x, y, z, partialTicks);
    }

    protected void extractRotatedQuad(
            QuadParticleRenderState state,
            Quaternionf rotation,
            float x,
            float y,
            float z,
            float partialTicks) {
        state.add(
                this.getLayer(),
                x,
                y,
                z,
                rotation.x,
                rotation.y,
                rotation.z,
                rotation.w,
                this.getQuadSize(partialTicks),
                this.getU0(),
                this.getU1(),
                this.getV0(),
                this.getV1(),
                argb(this.alpha, this.rCol, this.gCol, this.bCol),
                this.getLightCoords(partialTicks));
    }

    static int argb(float a, float r, float g, float b) {
        return (Mth.floor(a * 255.0F) & 0xFF) << 24
                | (Mth.floor(r * 255.0F) & 0xFF) << 16
                | (Mth.floor(g * 255.0F) & 0xFF) << 8
                | (Mth.floor(b * 255.0F) & 0xFF);
    }

    // ---- 1.21.1 bridge -------------------------------------------------------------------

    @Override
    protected int getLightColor(float partialTicks) {
        return getLightCoords(partialTicks);
    }

    @Override
    public net.minecraft.client.particle.ParticleRenderType getRenderType() {
        ParticleRenderType group = getGroup();
        return group == ParticleRenderType.SINGLE_QUADS ? getLayer().particleRenderType() : group;
    }

    /** 1.21.1 per-particle path (only reached if something renders this particle directly). */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        QuadParticleRenderState state = new QuadParticleRenderState();
        extract(state, camera, partialTicks);
        state.emitAll(buffer);
    }

    /**
     * 26.x {@code SingleQuadParticle.Layer(boolean translucent, ResourceLocation textureAtlasLocation,
     * RenderPipeline pipeline)}.
     *
     * <p>backport: holds the 1.21.1 {@link RenderType} the layer's quads are drawn with instead of a
     * pipeline (vertex format PARTICLE: position, uv, color, light). The vanilla layers are rebuilt
     * from the 1.21.1 particle sheet states; custom layers are built by the tree from its pipelines.
     */
    public static final class Layer {
        public static final Layer OPAQUE_TERRAIN =
                vanilla("terrain_opaque", false, TextureAtlas.LOCATION_BLOCKS);
        public static final Layer TRANSLUCENT_TERRAIN =
                vanilla("terrain_translucent", true, TextureAtlas.LOCATION_BLOCKS);
        // backport: 1.21.1 has no separate item atlas; item sprites live in the block atlas
        public static final Layer OPAQUE_ITEMS =
                vanilla("items_opaque", false, TextureAtlas.LOCATION_BLOCKS);
        public static final Layer TRANSLUCENT_ITEMS =
                vanilla("items_translucent", true, TextureAtlas.LOCATION_BLOCKS);
        public static final Layer OPAQUE =
                vanilla("particles_opaque", false, TextureAtlas.LOCATION_PARTICLES);
        public static final Layer TRANSLUCENT =
                vanilla("particles_translucent", true, TextureAtlas.LOCATION_PARTICLES);

        private final boolean translucent;
        private final ResourceLocation textureAtlasLocation;
        private final RenderType renderType;
        private final LayerType particleRenderType;

        public Layer(boolean translucent, ResourceLocation textureAtlasLocation, RenderType renderType) {
            this.translucent = translucent;
            this.textureAtlasLocation = textureAtlasLocation;
            this.renderType = renderType;
            this.particleRenderType = new LayerType(this);
        }

        public boolean translucent() {
            return translucent;
        }

        public ResourceLocation textureAtlasLocation() {
            return textureAtlasLocation;
        }

        public RenderType renderType() {
            return renderType;
        }

        /** The 1.21.1 engine queue key of particles drawn in this layer. */
        public net.minecraft.client.particle.ParticleRenderType particleRenderType() {
            return particleRenderType;
        }

        @Override
        public String toString() {
            return "Layer[" + renderType + "]";
        }

        private static Layer vanilla(String name, boolean translucent, ResourceLocation atlas) {
            return new Layer(translucent, atlas, particleSheet("hbm_backport_" + name, translucent, atlas));
        }

        /**
         * A 1.21.1 RenderType equivalent to the vanilla particle sheet states ({@code
         * ParticleRenderType.PARTICLE_SHEET_OPAQUE / _TRANSLUCENT / TERRAIN_SHEET}): particle shader,
         * atlas, lightmap, depth write on, blending only when translucent.
         */
        public static RenderType particleSheet(String name, boolean translucent, ResourceLocation atlas) {
            return RenderType.create(
                    name,
                    DefaultVertexFormat.PARTICLE,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    translucent,
                    RenderType.CompositeState.builder()
                            .setShaderState(
                                    new RenderStateShard.ShaderStateShard(GameRenderer::getParticleShader))
                            .setTextureState(new RenderStateShard.TextureStateShard(atlas, false, false))
                            .setTransparencyState(
                                    translucent
                                            ? RenderStateShard.TRANSLUCENT_TRANSPARENCY
                                            : RenderStateShard.NO_TRANSPARENCY)
                            .setLightmapState(RenderStateShard.LIGHTMAP)
                            .createCompositeState(false));
        }
    }

    /** backport: the 1.21.1 engine queue of one layer; renders all its particles itself. */
    static final class LayerType implements net.minecraft.client.particle.ParticleRenderType {
        final Layer layer;

        LayerType(Layer layer) {
            this.layer = layer;
        }

        @Override
        public @Nullable BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            ParticleBridge.renderLayer(this);
            return null;
        }

        @Override
        public boolean isTranslucent() {
            return layer.translucent();
        }

        @Override
        public String toString() {
            return "SINGLE_QUADS/" + layer.renderType();
        }
    }
}
