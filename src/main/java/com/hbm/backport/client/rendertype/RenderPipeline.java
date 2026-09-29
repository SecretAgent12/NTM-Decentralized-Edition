// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 26.x {@code com.mojang.blaze3d.pipeline.RenderPipeline}: an immutable description of shaders, defines,
 * blending, depth, culling, vertex layout and topology. backport: 1.21.1 has no pipeline objects; this
 * class only describes, and {@link RenderTypeFactory} turns it (with a {@link RenderSetup}) into a 1.21.1
 * {@code RenderType.CompositeState}: shader shard (see {@link HbmShaders}), transparency, depth test,
 * write mask, cull and layering shards. Subclassable with the 26.x constructor (WorldRenderPipeline,
 * TracerRibbon.Pipeline); {@link #getShaderDefines()} is read virtually.
 */
public class RenderPipeline {
    private static final AtomicInteger SORT_KEYS = new AtomicInteger();

    private final ResourceLocation location;
    private final ResourceLocation vertexShader;
    private final ResourceLocation fragmentShader;
    private final ShaderDefines shaderDefines;
    private final List<BindGroupLayout> bindGroupLayouts;
    private final List<ColorTargetState> colorTargetStates;
    private final @Nullable DepthStencilState depthStencilState;
    private final PolygonMode polygonMode;
    private final boolean cull;
    /** 26.x public field: the vertex format bound to each vertex buffer slot (slot 0 only here). */
    public final List<VertexFormat> vertexFormatPerBuffer;
    private final PrimitiveTopology primitiveTopology;
    private final int sortKey;

    public RenderPipeline(
            ResourceLocation location,
            ResourceLocation vertexShader,
            ResourceLocation fragmentShader,
            ShaderDefines shaderDefines,
            List<BindGroupLayout> bindGroupLayouts,
            List<ColorTargetState> colorTargetStates,
            @Nullable DepthStencilState depthStencilState,
            PolygonMode polygonMode,
            boolean cull,
            List<VertexFormat> vertexFormatPerBuffer,
            PrimitiveTopology primitiveTopology,
            int sortKey) {
        this.location = location;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.shaderDefines = shaderDefines;
        this.bindGroupLayouts = List.copyOf(bindGroupLayouts);
        this.colorTargetStates = List.copyOf(colorTargetStates);
        this.depthStencilState = depthStencilState;
        this.polygonMode = polygonMode;
        this.cull = cull;
        this.vertexFormatPerBuffer = List.copyOf(vertexFormatPerBuffer);
        this.primitiveTopology = primitiveTopology;
        this.sortKey = sortKey;
    }

    public static Builder builder(Snippet... snippets) {
        Builder b = new Builder();
        for (Snippet s : snippets) b.withSnippet(s);
        return b;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public ResourceLocation getVertexShader() {
        return vertexShader;
    }

    public ResourceLocation getFragmentShader() {
        return fragmentShader;
    }

    public ShaderDefines getShaderDefines() {
        return shaderDefines;
    }

    public List<BindGroupLayout> getBindGroupLayouts() {
        return bindGroupLayouts;
    }

    /** backport: the sampler names of all bind groups. */
    public List<String> getSamplers() {
        return BindGroupLayout.flattenSamplers(bindGroupLayouts);
    }

    public List<ColorTargetState> getColorTargetStates() {
        return colorTargetStates;
    }

    public ColorTargetState getColorTargetState() {
        return colorTargetStates.isEmpty() ? ColorTargetState.DEFAULT : colorTargetStates.get(0);
    }

    public @Nullable DepthStencilState getDepthStencilState() {
        return depthStencilState;
    }

    public PolygonMode getPolygonMode() {
        return polygonMode;
    }

    public boolean isCull() {
        return cull;
    }

    public VertexFormat getVertexFormat() {
        return vertexFormatPerBuffer.get(0);
    }

    public PrimitiveTopology getPrimitiveTopology() {
        return primitiveTopology;
    }

    /** 1.21.5-style accessor: the 1.21.1 draw mode. */
    public VertexFormat.Mode getVertexFormatMode() {
        return primitiveTopology.mode();
    }

    public int getSortKey() {
        return sortKey;
    }

    /** 26.x per-binding vertex format (only binding 0 exists). */
    public VertexFormat getVertexFormatBinding(int binding) {
        return vertexFormatPerBuffer.get(binding);
    }

    /**
     * backport: sets the 1.21.1 GL blend state of this pipeline (GUI blits, which 1.21.1 draws
     * immediately). Pair with {@link #clearBlend()}.
     */
    public void applyBlend() {
        Optional<BlendFunction> blend = getColorTargetState().blendFunction();
        if (blend.isPresent()) blend.get().apply();
        else com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    public void clearBlend() {
        Optional<BlendFunction> blend = getColorTargetState().blendFunction();
        if (blend.isPresent()) blend.get().clear();
    }

    @Override
    public String toString() {
        return location.toString();
    }

    /** 26.x {@code RenderPipeline.Snippet}: a partial pipeline builders start from. */
    public record Snippet(
            Optional<ResourceLocation> vertexShader,
            Optional<ResourceLocation> fragmentShader,
            ShaderDefines shaderDefines,
            List<BindGroupLayout> bindGroupLayouts,
            Optional<ColorTargetState> colorTargetState,
            Optional<Optional<DepthStencilState>> depthStencilState,
            Optional<PolygonMode> polygonMode,
            Optional<Boolean> cull,
            Optional<VertexFormat> vertexFormat,
            Optional<PrimitiveTopology> primitiveTopology) {}

    /** 26.x {@code RenderPipeline.Builder}. */
    public static class Builder {
        private Optional<ResourceLocation> location = Optional.empty();
        private Optional<ResourceLocation> vertexShader = Optional.empty();
        private Optional<ResourceLocation> fragmentShader = Optional.empty();
        private final ShaderDefines.Builder defines = ShaderDefines.builder();
        private final List<BindGroupLayout> bindGroupLayouts = new ArrayList<>();
        private Optional<ColorTargetState> colorTargetState = Optional.empty();
        private Optional<Optional<DepthStencilState>> depthStencilState = Optional.empty();
        private Optional<PolygonMode> polygonMode = Optional.empty();
        private Optional<Boolean> cull = Optional.empty();
        private Optional<VertexFormat> vertexFormat = Optional.empty();
        private Optional<PrimitiveTopology> primitiveTopology = Optional.empty();
        private float depthBiasScale;
        private float depthBiasConstant;

        protected Builder() {}

        void withSnippet(Snippet s) {
            if (s.vertexShader().isPresent()) vertexShader = s.vertexShader();
            if (s.fragmentShader().isPresent()) fragmentShader = s.fragmentShader();
            defines.addAll(s.shaderDefines());
            for (BindGroupLayout l : s.bindGroupLayouts()) if (!bindGroupLayouts.contains(l)) bindGroupLayouts.add(l);
            if (s.colorTargetState().isPresent()) colorTargetState = s.colorTargetState();
            if (s.depthStencilState().isPresent()) depthStencilState = s.depthStencilState();
            if (s.polygonMode().isPresent()) polygonMode = s.polygonMode();
            if (s.cull().isPresent()) cull = s.cull();
            if (s.vertexFormat().isPresent()) vertexFormat = s.vertexFormat();
            if (s.primitiveTopology().isPresent()) primitiveTopology = s.primitiveTopology();
        }

        public Builder withLocation(String location) {
            return withLocation(ResourceLocation.withDefaultNamespace(location));
        }

        public Builder withLocation(ResourceLocation location) {
            this.location = Optional.of(location);
            return this;
        }

        public Builder withVertexShader(String shader) {
            return withVertexShader(ResourceLocation.withDefaultNamespace(shader));
        }

        public Builder withVertexShader(ResourceLocation shader) {
            this.vertexShader = Optional.of(shader);
            return this;
        }

        public Builder withFragmentShader(String shader) {
            return withFragmentShader(ResourceLocation.withDefaultNamespace(shader));
        }

        public Builder withFragmentShader(ResourceLocation shader) {
            this.fragmentShader = Optional.of(shader);
            return this;
        }

        public Builder withShaderDefine(String flag) {
            defines.define(flag);
            return this;
        }

        public Builder withShaderDefine(String name, int value) {
            defines.define(name, value);
            return this;
        }

        public Builder withShaderDefine(String name, float value) {
            defines.define(name, value);
            return this;
        }

        public Builder withBindGroupLayout(BindGroupLayout layout) {
            if (!bindGroupLayouts.contains(layout)) bindGroupLayouts.add(layout);
            return this;
        }

        /** 1.21.5-style: a single sampler. */
        public Builder withSampler(String sampler) {
            return withBindGroupLayout(BindGroupLayout.samplers(sampler.toLowerCase(java.util.Locale.ROOT), sampler));
        }

        public Builder withColorTargetState(ColorTargetState state) {
            this.colorTargetState = Optional.of(state);
            return this;
        }

        /** 1.21.5-style: blending on the current color target. */
        public Builder withBlend(BlendFunction blend) {
            return withColorTargetState(new ColorTargetState(blend));
        }

        public Builder withoutBlend() {
            return withColorTargetState(ColorTargetState.DEFAULT);
        }

        public Builder withDepthStencilState(@Nullable DepthStencilState state) {
            this.depthStencilState = Optional.of(Optional.ofNullable(state));
            return this;
        }

        public Builder withDepthBias(float scaleFactor, float constant) {
            this.depthBiasScale = scaleFactor;
            this.depthBiasConstant = constant;
            return this;
        }

        public Builder withPolygonMode(PolygonMode mode) {
            this.polygonMode = Optional.of(mode);
            return this;
        }

        public Builder withCull(boolean cull) {
            this.cull = Optional.of(cull);
            return this;
        }

        public Builder withVertexBinding(int binding, VertexFormat format) {
            if (binding != 0)
                throw new IllegalArgumentException("backport: only vertex buffer binding 0 exists in 1.21.1");
            this.vertexFormat = Optional.of(format);
            return this;
        }

        public Builder withPrimitiveTopology(PrimitiveTopology topology) {
            this.primitiveTopology = Optional.of(topology);
            return this;
        }

        /** 1.21.5-style: vertex format and mode together. */
        public Builder withVertexFormat(VertexFormat format, VertexFormat.Mode mode) {
            this.vertexFormat = Optional.of(format);
            this.primitiveTopology = Optional.of(PrimitiveTopology.of(mode));
            return this;
        }

        public Snippet buildSnippet() {
            return new Snippet(
                    vertexShader,
                    fragmentShader,
                    defines.build(),
                    List.copyOf(bindGroupLayouts),
                    colorTargetState,
                    depthStencilState,
                    polygonMode,
                    cull,
                    vertexFormat,
                    primitiveTopology);
        }

        public RenderPipeline build() {
            ResourceLocation loc = location.orElseThrow(() -> new IllegalStateException("Missing location"));
            DepthStencilState depth = depthStencilState.flatMap(d -> d).orElse(null);
            if (depth != null && (depthBiasScale != 0F || depthBiasConstant != 0F))
                depth = new DepthStencilState(depth.depthTest(), depth.writeDepth(), depthBiasScale, depthBiasConstant);
            return new RenderPipeline(
                    loc,
                    vertexShader.orElseThrow(() -> new IllegalStateException("Missing vertex shader: " + loc)),
                    fragmentShader.orElseThrow(() -> new IllegalStateException("Missing fragment shader: " + loc)),
                    defines.build(),
                    bindGroupLayouts,
                    List.of(colorTargetState.orElse(ColorTargetState.DEFAULT)),
                    depth,
                    polygonMode.orElse(PolygonMode.FILL),
                    cull.orElse(true),
                    List.of(vertexFormat.orElseThrow(() -> new IllegalStateException("Missing vertex format: " + loc))),
                    primitiveTopology.orElseThrow(() -> new IllegalStateException("Missing topology: " + loc)),
                    SORT_KEYS.getAndIncrement());
        }
    }
}
