// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.gui.render.TextureSetup}: the texture a GUI element samples. On
 * 1.21.1 a texture is a GL id (sampler state lives on the texture), so the 26.x
 * {@code singleTexture(texture.getTextureView(), texture.getSampler())} becomes
 * {@code singleTexture(texture)} .
 */
public final class TextureSetup {

    private static final TextureSetup NONE = new TextureSetup(null, null);

    private final @Nullable AbstractTexture texture;
    private final @Nullable ResourceLocation location;

    private TextureSetup(@Nullable AbstractTexture texture, @Nullable ResourceLocation location) {
        this.texture = texture;
        this.location = location;
    }

    public static TextureSetup noTexture() {
        return NONE;
    }

    public static TextureSetup singleTexture(AbstractTexture texture) {
        return new TextureSetup(texture, null);
    }

    public static TextureSetup singleTexture(AbstractTexture texture, @Nullable Object sampler) {
        return new TextureSetup(texture, null);
    }

    public static TextureSetup singleTexture(ResourceLocation location) {
        return new TextureSetup(null, location);
    }

    public boolean isEmpty() {
        return texture == null && location == null;
    }

    /** The GL texture id to bind, or -1. */
    public int glId() {
        if (texture != null) return texture.getId();
        if (location != null) return Minecraft.getInstance().getTextureManager().getTexture(location).getId();
        return -1;
    }
}
