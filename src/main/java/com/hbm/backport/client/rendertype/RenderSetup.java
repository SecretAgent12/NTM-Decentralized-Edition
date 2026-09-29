// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.rendertype.RenderSetup}: the per-RenderType part of a draw
 * (pipeline, bound textures, lightmap/overlay, crumbling, sorting, outline, layering, output target,
 * texture transform). backport: {@link RenderTypeFactory#create(String, RenderSetup)} turns it into a 1.21.1
 * RenderType (26.x {@code RenderType.create(name, setup)} is rewritten to that).
 */
public final class RenderSetup {
    public final RenderPipeline pipeline;
    public final Map<String, TextureBinding> textures;
    public final boolean useLightmap;
    public final boolean useOverlay;
    public final boolean affectsCrumbling;
    public final boolean sortOnUpload;
    public final int bufferSize;
    public final OutlineProperty outlineProperty;
    public final LayeringTransform layeringTransform;
    public final OutputTarget outputTarget;
    public final @Nullable TextureTransform textureTransform;

    private RenderSetup(Builder b) {
        this.pipeline = b.pipeline;
        this.textures = Collections.unmodifiableMap(new LinkedHashMap<>(b.textures));
        this.useLightmap = b.useLightmap;
        this.useOverlay = b.useOverlay;
        this.affectsCrumbling = b.affectsCrumbling;
        this.sortOnUpload = b.sortOnUpload;
        this.bufferSize = b.bufferSize;
        this.outlineProperty = b.outlineProperty;
        this.layeringTransform = b.layeringTransform;
        this.outputTarget = b.outputTarget;
        this.textureTransform = b.textureTransform;
    }

    public static Builder builder(RenderPipeline pipeline) {
        return new Builder(pipeline);
    }

    public RenderPipeline pipeline() {
        return pipeline;
    }

    /** backport: the texture bound to {@code sampler}, or null. */
    public @Nullable ResourceLocation texture(String sampler) {
        TextureBinding t = textures.get(sampler);
        return t == null ? null : t.location();
    }

    /** 26.x texture binding of a sampler. */
    public record TextureBinding(ResourceLocation location, boolean blur, boolean mipmap) {
        public TextureBinding(ResourceLocation location) {
            this(location, false, false);
        }
    }

    /** 26.x {@code RenderSetup.OutlineProperty}; {@link #vanilla()} is the 1.21.1 one. */
    public enum OutlineProperty {
        NONE(RenderType.OutlineProperty.NONE),
        IS_OUTLINE(RenderType.OutlineProperty.IS_OUTLINE),
        AFFECTS_OUTLINE(RenderType.OutlineProperty.AFFECTS_OUTLINE);

        private final RenderType.OutlineProperty vanilla;

        OutlineProperty(RenderType.OutlineProperty vanilla) {
            this.vanilla = vanilla;
        }

        public RenderType.OutlineProperty vanilla() {
            return vanilla;
        }
    }

    public static final class Builder {
        private final RenderPipeline pipeline;
        private final Map<String, TextureBinding> textures = new LinkedHashMap<>();
        private boolean useLightmap;
        private boolean useOverlay;
        private boolean affectsCrumbling;
        private boolean sortOnUpload;
        private int bufferSize = RenderType.TRANSIENT_BUFFER_SIZE;
        private OutlineProperty outlineProperty = OutlineProperty.NONE;
        private LayeringTransform layeringTransform = LayeringTransform.NO_LAYERING;
        private OutputTarget outputTarget = OutputTarget.MAIN_TARGET;
        private @Nullable TextureTransform textureTransform;

        private Builder(RenderPipeline pipeline) {
            this.pipeline = pipeline;
        }

        public Builder withTexture(String sampler, ResourceLocation texture) {
            textures.put(sampler, new TextureBinding(texture));
            return this;
        }

        /** backport: explicit blur/mipmap filtering (26.x takes a sampler object). */
        public Builder withTexture(String sampler, ResourceLocation texture, boolean blur, boolean mipmap) {
            textures.put(sampler, new TextureBinding(texture, blur, mipmap));
            return this;
        }

        public Builder useLightmap() {
            useLightmap = true;
            return this;
        }

        public Builder useOverlay() {
            useOverlay = true;
            return this;
        }

        public Builder affectsCrumbling() {
            affectsCrumbling = true;
            return this;
        }

        public Builder sortOnUpload() {
            sortOnUpload = true;
            return this;
        }

        public Builder bufferSize(int size) {
            bufferSize = size;
            return this;
        }

        public Builder setOutline(OutlineProperty outline) {
            outlineProperty = outline;
            return this;
        }

        public Builder setLayeringTransform(LayeringTransform layering) {
            layeringTransform = layering;
            return this;
        }

        public Builder setOutputTarget(OutputTarget target) {
            outputTarget = target;
            return this;
        }

        public Builder setTextureTransform(TextureTransform transform) {
            textureTransform = transform;
            return this;
        }

        public RenderSetup createRenderSetup() {
            return new RenderSetup(this);
        }
    }
}
