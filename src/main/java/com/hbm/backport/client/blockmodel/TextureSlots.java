// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * 26.x net.minecraft.client.resources.model.sprite.TextureSlots: resolved texture references of
 * a model (name without '#' -> Material). Backed by the 1.21.1 BlockModel's texture map or a
 * geometry baking context.
 */
public final class TextureSlots {

    public static final TextureSlots EMPTY = new TextureSlots(name -> null);

    private final Function<String, @Nullable Material> lookup;

    public TextureSlots(Function<String, @Nullable Material> lookup) {
        this.lookup = lookup;
    }

    public @Nullable Material getMaterial(String name) {
        if (name.startsWith("#")) name = name.substring(1);
        return lookup.apply(name);
    }

    public static TextureSlots of(net.minecraft.client.renderer.block.model.BlockModel model) {
        return new TextureSlots(name -> model.hasTexture(name) ? Material.of(model.getMaterial(name)) : null);
    }

    public static TextureSlots of(net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext context) {
        return new TextureSlots(name -> context.hasMaterial(name) ? Material.of(context.getMaterial(name)) : null);
    }
}
