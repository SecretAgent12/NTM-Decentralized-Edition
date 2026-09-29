// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * backport: what 26.x code read off a RenderType ({@code type.state.textures.get("Sampler0").location()},
 * {@code type.pipeline()}), for 1.21.1 RenderTypes. Types made by {@link RenderTypeFactory} answer from
 * their {@link RenderSetup}; vanilla ones from their composite state (read reflectively; NeoForge 1.21.1
 * runs with Mojang names).
 */
public final class RenderTypeInfo {
    private static final @Nullable Method STATE;
    private static final @Nullable Field TEXTURE_STATE;
    private static final @Nullable Method CUTOUT_TEXTURE;

    static {
        Method state = null;
        Field textureState = null;
        Method cutoutTexture = null;
        try {
            Class<?> composite = Class.forName("net.minecraft.client.renderer.RenderType$CompositeRenderType");
            state = composite.getDeclaredMethod("state");
            state.setAccessible(true);
            textureState = RenderType.CompositeState.class.getDeclaredField("textureState");
            textureState.setAccessible(true);
            cutoutTexture = RenderStateShard.EmptyTextureStateShard.class.getDeclaredMethod("cutoutTexture");
            cutoutTexture.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("backport: RenderType introspection unavailable", e);
        }
        STATE = state;
        TEXTURE_STATE = textureState;
        CUTOUT_TEXTURE = cutoutTexture;
    }

    private RenderTypeInfo() {}

    /** The pipeline of a factory-made type, or null for vanilla types. */
    public static @Nullable RenderPipeline pipeline(RenderType type) {
        RenderSetup setup = RenderTypeFactory.setupOf(type);
        return setup == null ? null : setup.pipeline;
    }

    /** The texture bound to Sampler0 (26.x {@code type.state.textures.get("Sampler0").location()}). */
    public static @Nullable ResourceLocation texture(RenderType type) {
        RenderSetup setup = RenderTypeFactory.setupOf(type);
        if (setup != null) return setup.texture("Sampler0");
        if (STATE == null || TEXTURE_STATE == null || CUTOUT_TEXTURE == null) return null;
        try {
            Object state = STATE.invoke(type);
            Object tex = TEXTURE_STATE.get(state);
            @SuppressWarnings("unchecked")
            Optional<ResourceLocation> loc = (Optional<ResourceLocation>) CUTOUT_TEXTURE.invoke(tex);
            return loc.orElse(null);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Whether a vanilla glyph type is a text type sampling the red channel (26.x TEXT_GRAYSCALE pipeline). */
    public static boolean isTextIntensity(RenderType type) {
        return type.name.startsWith("text_intensity");
    }

    /** Whether a vanilla glyph type is a plain color text type (26.x TEXT pipeline). */
    public static boolean isText(RenderType type) {
        return type.name.startsWith("text") && !type.name.startsWith("text_intensity")
                && !type.name.startsWith("text_background");
    }
}
