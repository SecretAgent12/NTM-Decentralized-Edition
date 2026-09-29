// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * 26.x net.minecraft.client.resources.model.Material: a block-atlas sprite reference as a model
 * texture slot resolves to it, plus whether quads using it are forced translucent. (1.21.1's
 * Material is the 26.x SpriteId: atlas + texture; {@link #vanilla()} converts.)
 */
public record Material(ResourceLocation sprite, boolean forceTranslucent) {

    public Material(ResourceLocation sprite) {
        this(sprite, false);
    }

    public Material withForceTranslucent(boolean force) {
        return force == forceTranslucent ? this : new Material(sprite, force);
    }

    public net.minecraft.client.resources.model.Material vanilla() {
        return new net.minecraft.client.resources.model.Material(TextureAtlas.LOCATION_BLOCKS, sprite);
    }

    public static Material of(net.minecraft.client.resources.model.Material vanilla) {
        return new Material(vanilla.texture());
    }

    /** 26.x Material.Baked: the resolved sprite. */
    public record Baked(TextureAtlasSprite sprite, boolean forceTranslucent) {

        public Baked(TextureAtlasSprite sprite) {
            this(sprite, false);
        }

        public Transparency transparency() {
            return forceTranslucent ? Transparency.TRANSLUCENT : Transparency.of(sprite);
        }
    }
}
