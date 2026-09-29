// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;

/**
 * 26.x net.minecraft.client.resources.model.sprite.SpriteId: (atlas, texture) - what 1.21.1 calls
 * Material. {@link #lookup} replaces {@code Minecraft.getInstance().getAtlasManager().get(id)}.
 */
public record SpriteId(ResourceLocation atlasLocation, ResourceLocation texture) {

    public net.minecraft.client.resources.model.Material vanilla() {
        return new net.minecraft.client.resources.model.Material(atlasLocation, texture);
    }

    public TextureAtlasSprite sprite() {
        // 26.x AtlasManager knows every atlas; 1.21.1 ModelManager only the model atlases (blocks,
        // signs, banners, ...), while the particle atlas belongs to ParticleEngine. Every atlas is
        // registered with the TextureManager, so look it up there (no NPE for the particle atlas)
        var registered = Minecraft.getInstance().getTextureManager().getTexture(atlasLocation);
        if (registered instanceof net.minecraft.client.renderer.texture.TextureAtlas atlas)
            return atlas.getSprite(texture);
        return Minecraft.getInstance().getTextureAtlas(atlasLocation).apply(texture);
    }

    public VertexConsumer buffer(MultiBufferSource buffers, Function<ResourceLocation, RenderType> type) {
        return vanilla().buffer(buffers, type);
    }

    public static TextureAtlasSprite lookup(SpriteId id) {
        return id.sprite();
    }

    public static SpriteId of(net.minecraft.client.resources.model.Material vanilla) {
        return new SpriteId(vanilla.atlasLocation(), vanilla.texture());
    }
}
