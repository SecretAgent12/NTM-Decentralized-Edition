// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Turns a 26.x {@link RenderSetup} (pipeline + per-type state) into a 1.21.1 {@link RenderType}: 26.x
 * {@code RenderType.create(name, setup)} is rewritten to {@link #create(String, RenderSetup)}.
 *
 * <p>Mapping: vertex format / topology from the pipeline; shader from the pipeline's vertex+fragment shader
 * ids (see {@link #shader}); {@code Sampler0} binding -> texture shard; blend function -> transparency
 * shard; depth test/write + color write mask -> depth test and write-mask shards; cull; lightmap/overlay
 * from the setup; layering (or the pipeline depth bias as polygon offset); output target; texture
 * transform -> texturing shard; line topologies get a line-width shard.
 */
public final class RenderTypeFactory {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<RenderType, RenderSetup> SETUPS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<BlendFunction, RenderStateShard.TransparencyStateShard> TRANSPARENCY =
            new ConcurrentHashMap<>();
    private static final Map<CompareOp, RenderStateShard.DepthTestStateShard> DEPTH = new ConcurrentHashMap<>();
    private static final Map<Object, RenderType> BY_PIPELINE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static final RenderStateShard.WriteMaskStateShard NO_WRITE =
            new RenderStateShard.WriteMaskStateShard(false, false);

    private RenderTypeFactory() {}

    /** 26.x {@code RenderType.create(String, RenderSetup)}. */
    public static RenderType create(String name, RenderSetup setup) {
        RenderPipeline p = setup.pipeline;
        VertexFormat format = p.getVertexFormat();
        VertexFormat.Mode mode = p.getPrimitiveTopology().mode();
        RenderType.CompositeState.CompositeStateBuilder b = RenderType.CompositeState.builder();

        b.setShaderState(shader(p));

        RenderSetup.TextureBinding tex = setup.textures.get("Sampler0");
        if (tex != null) b.setTextureState(new RenderStateShard.TextureStateShard(tex.location(), tex.blur(), tex.mipmap()));
        for (String sampler : setup.textures.keySet())
            if (!sampler.equals("Sampler0") && WARNED.add(name + "/" + sampler))
                // backport: 1.21.1 RenderTypes bind one texture; Sampler1/2 are overlay/lightmap shards
                LOGGER.warn("backport: render type {} binds {}, which 1.21.1 cannot bind", name, sampler);

        ColorTargetState color = p.getColorTargetState();
        b.setTransparencyState(color.blendFunction().map(RenderTypeFactory::transparency)
                .orElse(RenderStateShard.NO_TRANSPARENCY));

        DepthStencilState depth = p.getDepthStencilState();
        b.setDepthTestState(depth == null ? RenderStateShard.NO_DEPTH_TEST : depthTest(depth.depthTest()));
        boolean writeColor = color.writeColor();
        boolean writeDepth = depth != null && depth.writeDepth();
        b.setWriteMaskState(writeColor
                ? (writeDepth ? RenderStateShard.COLOR_DEPTH_WRITE : RenderStateShard.COLOR_WRITE)
                : (writeDepth ? RenderStateShard.DEPTH_WRITE : NO_WRITE));

        b.setCullState(p.isCull() ? RenderStateShard.CULL : RenderStateShard.NO_CULL);
        b.setLightmapState(setup.useLightmap ? RenderStateShard.LIGHTMAP : RenderStateShard.NO_LIGHTMAP);
        b.setOverlayState(setup.useOverlay ? RenderStateShard.OVERLAY : RenderStateShard.NO_OVERLAY);

        if (setup.layeringTransform != LayeringTransform.NO_LAYERING) b.setLayeringState(setup.layeringTransform.shard());
        else if (depth != null && depth.hasDepthBias()) b.setLayeringState(depthBias(depth));

        b.setOutputState(setup.outputTarget.shard());
        if (setup.textureTransform != null) b.setTexturingState(setup.textureTransform.shard());
        if (p.getPrimitiveTopology().isLines()) b.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()));

        RenderType type = RenderType.create(
                name,
                format,
                mode,
                setup.bufferSize,
                setup.affectsCrumbling,
                setup.sortOnUpload,
                b.createCompositeState(setup.outlineProperty.vanilla()));
        SETUPS.put(type, setup);
        return type;
    }

    /** 1.21.1 {@code RenderType.create} (7 args): passes through (the create rewrite also hits these). */
    public static RenderType create(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            RenderType.CompositeState state) {
        return RenderType.create(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, state);
    }

    /** 1.21.1 {@code RenderType.create} (5 args): passes through. */
    public static RenderType create(
            String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, RenderType.CompositeState state) {
        return RenderType.create(name, format, mode, bufferSize, state);
    }

    /**
     * backport: a cached RenderType drawing {@code pipeline} with {@code texture} on Sampler0 (null for
     * untextured), using the lightmap / overlay when the pipeline samples Sampler2 / Sampler1. For callers
     * that hold a bare pipeline (GUI blits, particle layers).
     */
    public static RenderType forPipeline(RenderPipeline pipeline, @Nullable ResourceLocation texture) {
        return BY_PIPELINE.computeIfAbsent(
                texture == null ? pipeline : new PipelineTexture(pipeline, texture),
                k -> {
                    RenderSetup.Builder s = RenderSetup.builder(pipeline);
                    if (texture != null) s.withTexture("Sampler0", texture);
                    var samplers = pipeline.getSamplers();
                    if (samplers.contains("Sampler2")) s.useLightmap();
                    if (samplers.contains("Sampler1")) s.useOverlay();
                    if (pipeline.getColorTargetState().blendFunction().isPresent()
                            && pipeline.getPrimitiveTopology() == PrimitiveTopology.QUADS) s.sortOnUpload();
                    return create(pipeline.getLocation().toString(), s.createRenderSetup());
                });
    }

    private record PipelineTexture(RenderPipeline pipeline, ResourceLocation texture) {}

    /** backport: the setup a RenderType was created from by this factory, or null. */
    public static @Nullable RenderSetup setupOf(RenderType type) {
        return SETUPS.get(type);
    }

    // ---- shards ----

    private static RenderStateShard.TransparencyStateShard transparency(BlendFunction f) {
        return TRANSPARENCY.computeIfAbsent(
                f,
                k -> new RenderStateShard.TransparencyStateShard(
                        "hbm_blend_" + Integer.toHexString(k.hashCode()), k::apply, k::clear));
    }

    private static RenderStateShard.DepthTestStateShard depthTest(CompareOp op) {
        return switch (op) {
            case ALWAYS_PASS -> RenderStateShard.NO_DEPTH_TEST;
            case LESS_THAN_OR_EQUAL -> RenderStateShard.LEQUAL_DEPTH_TEST;
            case EQUAL -> RenderStateShard.EQUAL_DEPTH_TEST;
            case GREATER_THAN -> RenderStateShard.GREATER_DEPTH_TEST;
            default -> DEPTH.computeIfAbsent(op, k -> new RenderStateShard.DepthTestStateShard(
                    k.name().toLowerCase(java.util.Locale.ROOT), k.gl()));
        };
    }

    private static RenderStateShard.LayeringStateShard depthBias(DepthStencilState d) {
        float scale = d.depthBiasScaleFactor();
        float constant = d.depthBiasConstant();
        return new RenderStateShard.LayeringStateShard(
                "hbm_depth_bias",
                () -> {
                    RenderSystem.polygonOffset(scale, constant);
                    RenderSystem.enablePolygonOffset();
                },
                () -> {
                    RenderSystem.polygonOffset(0F, 0F);
                    RenderSystem.disablePolygonOffset();
                });
    }

    // ---- shaders ----

    /**
     * The 1.21.1 shader shard for a pipeline. 26.x shader ids -> 1.21.1 programs:
     * {@code core/entity}, {@code hbm:core/entity_fade}, {@code hbm:core/entity_nofog} -> hbm:entity(_fade,
     * _nofog) with ALPHA_CUTOUT / EMISSIVE / NO_OVERLAY / NO_CARDINAL_LIGHTING / APPLY_TEXTURE_MATRIX as
     * uniforms; particle family likewise (ALPHA_CUTOUT, default 0.1); lightning, lines, text, tracer ribbon;
     * plain vanilla programs (position*, gui, crumbling, lines, text) map to the 1.21.1 vanilla shaders.
     */
    public static RenderStateShard.ShaderStateShard shader(RenderPipeline p) {
        String vs = p.getVertexShader().toString();
        String fs = p.getFragmentShader().toString();
        ShaderDefines d = p.getShaderDefines();
        switch (fs) {
            case "minecraft:core/entity":
                return entity(HbmShaders::entity, d);
            case "hbm:core/entity_fade":
                return entity(HbmShaders::entityFade, d);
            case "hbm:core/entity_nofog":
                return entity(HbmShaders::entityNoFog, d);
            case "minecraft:core/particle":
                if (d.isEmpty()) return new RenderStateShard.ShaderStateShard(GameRenderer::getParticleShader);
                return particle(HbmShaders::particle, d);
            case "hbm:core/particle_fade":
                return particle(HbmShaders::particleFade, d);
            case "hbm:core/particle_cutout":
                return particle(HbmShaders::particleCutout, d);
            case "hbm:core/particle_nofog":
                return particle(HbmShaders::particleNoFog, d);
            case "minecraft:core/rendertype_lightning":
                return program(HbmShaders::lightning, GameRenderer::getRendertypeLightningShader, s -> {});
            case "hbm:core/rendertype_lightning_nofog":
                return program(HbmShaders::lightningNoFog, GameRenderer::getRendertypeLightningShader, s -> {});
            case "minecraft:core/rendertype_lines":
                return RenderStateShard.RENDERTYPE_LINES_SHADER;
            case "hbm:core/rendertype_lines_nofog":
                return program(HbmShaders::linesNoFog, GameRenderer::getRendertypeLinesShader, s -> {});
            case "minecraft:core/text":
                return d.has("IS_GRAYSCALE")
                        ? RenderStateShard.RENDERTYPE_TEXT_INTENSITY_SHADER
                        : RenderStateShard.RENDERTYPE_TEXT_SHADER;
            case "hbm:core/text_fade": {
                float gray = d.has("IS_GRAYSCALE") ? 1F : 0F;
                float cutout = alphaCutout(d, 0.1F);
                return program(HbmShaders::textFade, GameRenderer::getRendertypeTextShader, s -> {
                    s.safeGetUniform("HbmGrayscale").set(gray);
                    s.safeGetUniform("AlphaCutout").set(cutout);
                });
            }
            case "hbm:core/tracer_ribbon":
            case "hbm:core/tracer_ribbon_nofog": {
                float emissive = d.has("EMISSIVE") ? 1F : 0F;
                float minWidth = d.floatValue("MIN_RIBBON_WIDTH", 1F);
                float padding = d.floatValue("RIBBON_FILTER_PADDING", 1F);
                Supplier<ShaderInstance> prog =
                        fs.endsWith("_nofog") ? HbmShaders::tracerRibbonNoFog : HbmShaders::tracerRibbon;
                return program(prog, GameRenderer::getPositionColorShader, s -> {
                    s.safeGetUniform("HbmEmissive").set(emissive);
                    s.safeGetUniform("MinRibbonWidth").set(minWidth);
                    s.safeGetUniform("RibbonFilterPadding").set(padding);
                });
            }
            case "minecraft:core/position_color":
                return RenderStateShard.POSITION_COLOR_SHADER;
            case "minecraft:core/position":
            case "minecraft:core/stars":
                return RenderStateShard.POSITION_SHADER;
            case "minecraft:core/position_tex":
                return RenderStateShard.POSITION_TEX_SHADER;
            case "minecraft:core/position_tex_color":
                return new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);
            case "minecraft:core/position_color_lightmap":
                return RenderStateShard.POSITION_COLOR_LIGHTMAP_SHADER;
            case "minecraft:core/position_color_tex_lightmap":
                return RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER;
            case "minecraft:core/gui":
                return RenderStateShard.RENDERTYPE_GUI_SHADER;
            case "minecraft:core/rendertype_crumbling":
                return RenderStateShard.RENDERTYPE_CRUMBLING_SHADER;
            case "minecraft:core/glint":
            case "minecraft:core/rendertype_entity_glint":
                return RenderStateShard.RENDERTYPE_ENTITY_GLINT_SHADER;
            case "minecraft:core/rendertype_armor_entity_glint":
                return RenderStateShard.RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER;
            case "minecraft:core/rendertype_leash":
                return RenderStateShard.RENDERTYPE_LEASH_SHADER;
            case "minecraft:core/rendertype_water_mask":
                return RenderStateShard.RENDERTYPE_WATER_MASK_SHADER;
            case "minecraft:core/rendertype_beacon_beam":
                return RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER;
            case "minecraft:core/rendertype_end_portal":
                return RenderStateShard.RENDERTYPE_END_PORTAL_SHADER;
            case "minecraft:core/terrain":
            case "minecraft:core/block":
                return d.has("ALPHA_CUTOUT")
                        ? RenderStateShard.RENDERTYPE_CUTOUT_SHADER
                        : p.getColorTargetState().blendFunction().isPresent()
                                ? RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER
                                : RenderStateShard.RENDERTYPE_SOLID_SHADER;
            default:
                return fallback(p, vs + " / " + fs);
        }
    }

    private static RenderStateShard.ShaderStateShard fallback(RenderPipeline p, String what) {
        if (WARNED.add(what))
            LOGGER.warn("backport: no 1.21.1 shader for pipeline {} ({}); using a format-matched vanilla one",
                    p.getLocation(), what);
        VertexFormat f = p.getVertexFormat();
        if (f == DefaultVertexFormat.NEW_ENTITY) return entity(HbmShaders::entity, p.getShaderDefines());
        if (f == DefaultVertexFormat.PARTICLE) return particle(HbmShaders::particle, p.getShaderDefines());
        if (f == DefaultVertexFormat.POSITION_COLOR_NORMAL) return RenderStateShard.RENDERTYPE_LINES_SHADER;
        if (f == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP) return RenderStateShard.RENDERTYPE_TEXT_SHADER;
        if (f == DefaultVertexFormat.POSITION_TEX) return RenderStateShard.POSITION_TEX_SHADER;
        if (f == DefaultVertexFormat.POSITION_TEX_COLOR)
            return new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);
        if (f == DefaultVertexFormat.POSITION) return RenderStateShard.POSITION_SHADER;
        if (f == DefaultVertexFormat.BLOCK) return RenderStateShard.RENDERTYPE_SOLID_SHADER;
        return RenderStateShard.POSITION_COLOR_SHADER;
    }

    private static float alphaCutout(ShaderDefines d, float absent) {
        if (d.values().containsKey("ALPHA_CUTOUT")) return d.floatValue("ALPHA_CUTOUT", absent);
        if (d.flags().contains("ALPHA_CUTOUT")) return 0.1F;
        return absent;
    }

    /** backport: the 26.x entity shader's defines as the backport programs' uniforms. */
    private static RenderStateShard.ShaderStateShard entity(Supplier<ShaderInstance> program, ShaderDefines d) {
        float cardinal = d.has("NO_CARDINAL_LIGHTING") ? 0F : 1F;
        float lightmap = d.has("EMISSIVE") ? 0F : 1F;
        float overlay = d.has("NO_OVERLAY") ? 0F : 1F;
        float textureMatrix = d.has("APPLY_TEXTURE_MATRIX") ? 1F : 0F;
        // no ALPHA_CUTOUT: nothing is discarded (texture alpha is never below -1)
        float cutout = alphaCutout(d, -1F);
        return program(program, GameRenderer::getRendertypeEntityTranslucentShader, s -> {
            s.safeGetUniform("HbmFlags").set(cardinal, lightmap, overlay, textureMatrix);
            s.safeGetUniform("AlphaCutout").set(cutout);
        });
    }

    private static RenderStateShard.ShaderStateShard particle(Supplier<ShaderInstance> program, ShaderDefines d) {
        float cutout = alphaCutout(d, 0.1F);
        return program(program, GameRenderer::getParticleShader,
                s -> s.safeGetUniform("AlphaCutout").set(cutout));
    }

    /**
     * A shader shard for a backport program: RenderSystem.setShader calls the supplier during setup, so the
     * per-type uniforms are written right before the draw uploads them. Falls back to a vanilla shader
     * while the program is missing (failed to load).
     */
    private static RenderStateShard.ShaderStateShard program(
            Supplier<ShaderInstance> program, Supplier<ShaderInstance> fallback, Consumer<ShaderInstance> uniforms) {
        return new RenderStateShard.ShaderStateShard(() -> {
            ShaderInstance s = program.get();
            if (s == null) return fallback.get();
            uniforms.accept(s);
            return s;
        });
    }
}
