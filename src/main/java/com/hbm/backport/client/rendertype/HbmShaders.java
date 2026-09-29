// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The mod's core shader programs in 1.21.1 form ({@code assets/hbm/shaders/core/<name>.json}), loaded
 * through NeoForge's {@link RegisterShadersEvent}. They stand in for 26.x's define-driven shaders:
 * vanilla {@code core/entity} and hbm {@code entity_fade}/{@code entity_nofog} (ENTITY format),
 * {@code core/particle} and hbm {@code particle_fade/_cutout/_nofog}, {@code core/rendertype_lightning}
 * and hbm {@code rendertype_lightning_nofog}, hbm {@code rendertype_lines_nofog}, hbm {@code text_fade},
 * hbm {@code tracer_ribbon(_nofog)}. The 26.x defines become uniforms that {@link RenderTypeFactory} sets
 * per RenderType ({@code HbmFlags}, {@code AlphaCutout}, {@code HbmGrayscale}, {@code HbmEmissive},
 * {@code MinRibbonWidth}, {@code RibbonFilterPadding}).
 */
public final class HbmShaders {
    private static final Logger LOGGER = LogUtils.getLogger();

    static @Nullable ShaderInstance entity;
    static @Nullable ShaderInstance entityFade;
    static @Nullable ShaderInstance entityNoFog;
    static @Nullable ShaderInstance particle;
    static @Nullable ShaderInstance particleFade;
    static @Nullable ShaderInstance particleCutout;
    static @Nullable ShaderInstance particleNoFog;
    static @Nullable ShaderInstance lightning;
    static @Nullable ShaderInstance lightningNoFog;
    static @Nullable ShaderInstance linesNoFog;
    static @Nullable ShaderInstance textFade;
    static @Nullable ShaderInstance tracerRibbon;
    static @Nullable ShaderInstance tracerRibbonNoFog;

    /** The tracer ribbon vertex layout (26.x TracerRibbon.FORMAT, same attribute names and order). */
    public static final VertexFormat TRACER_RIBBON_FORMAT =
            GpuFormat.formatBuilder()
                    .addAttribute("Position", GpuFormat.RGB32_FLOAT)
                    .addAttribute("OtherPosition", GpuFormat.RGB32_FLOAT)
                    .addAttribute("Color", GpuFormat.RGBA8_UNORM)
                    .addAttribute("OtherColor", GpuFormat.RGBA8_UNORM)
                    .addAttribute("Widths", GpuFormat.RG32_FLOAT)
                    .addAttribute("UV0", GpuFormat.RG32_FLOAT)
                    .addAttribute("UV2", GpuFormat.RG16_SINT)
                    .build();

    private HbmShaders() {}

    /** Hook for the client entry point (mod event bus). */
    public static void register(IEventBus modBus) {
        modBus.addListener(HbmShaders::onRegisterShaders);
    }

    private static void onRegisterShaders(RegisterShadersEvent event) {
        load(event, "entity", DefaultVertexFormat.NEW_ENTITY, s -> entity = s);
        load(event, "entity_fade", DefaultVertexFormat.NEW_ENTITY, s -> entityFade = s);
        load(event, "entity_nofog", DefaultVertexFormat.NEW_ENTITY, s -> entityNoFog = s);
        load(event, "particle", DefaultVertexFormat.PARTICLE, s -> particle = s);
        load(event, "particle_fade", DefaultVertexFormat.PARTICLE, s -> particleFade = s);
        load(event, "particle_cutout", DefaultVertexFormat.PARTICLE, s -> particleCutout = s);
        load(event, "particle_nofog", DefaultVertexFormat.PARTICLE, s -> particleNoFog = s);
        load(event, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR, s -> lightning = s);
        load(event, "rendertype_lightning_nofog", DefaultVertexFormat.POSITION_COLOR, s -> lightningNoFog = s);
        load(event, "rendertype_lines_nofog", DefaultVertexFormat.POSITION_COLOR_NORMAL, s -> linesNoFog = s);
        load(event, "text_fade", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, s -> textFade = s);
        load(event, "tracer_ribbon", TRACER_RIBBON_FORMAT, s -> tracerRibbon = s);
        load(event, "tracer_ribbon_nofog", TRACER_RIBBON_FORMAT, s -> tracerRibbonNoFog = s);
    }

    private static void load(
            RegisterShadersEvent event, String name, VertexFormat format, Consumer<ShaderInstance> sink) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("hbm", name), format),
                    sink);
        } catch (IOException e) {
            LOGGER.error("Failed to load hbm shader {}", name, e);
        }
    }

    public static @Nullable ShaderInstance entity() {
        return entity;
    }

    public static @Nullable ShaderInstance entityFade() {
        return entityFade;
    }

    public static @Nullable ShaderInstance entityNoFog() {
        return entityNoFog;
    }

    public static @Nullable ShaderInstance particle() {
        return particle;
    }

    public static @Nullable ShaderInstance particleFade() {
        return particleFade;
    }

    public static @Nullable ShaderInstance particleCutout() {
        return particleCutout;
    }

    public static @Nullable ShaderInstance particleNoFog() {
        return particleNoFog;
    }

    public static @Nullable ShaderInstance lightning() {
        return lightning;
    }

    public static @Nullable ShaderInstance lightningNoFog() {
        return lightningNoFog;
    }

    public static @Nullable ShaderInstance linesNoFog() {
        return linesNoFog;
    }

    public static @Nullable ShaderInstance textFade() {
        return textFade;
    }

    public static @Nullable ShaderInstance tracerRibbon() {
        return tracerRibbon;
    }

    public static @Nullable ShaderInstance tracerRibbonNoFog() {
        return tracerRibbonNoFog;
    }
}
