// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModelBaker} of one model reload: models from the 1.21.1 model bakery (parent
 * chains resolved on first use), sprites from the reload's block atlas, shared results per key.
 */
public final class Baker implements ModelBaker, ModelBaker.Materials {
    private static final Logger LOGGER = LoggerFactory.getLogger("hbm/blockmodel");

    private final Function<ResourceLocation, UnbakedModel> models;
    private final Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> sprites;
    private final Map<ResourceLocation, ResolvedModel> resolved = new ConcurrentHashMap<>();
    private final Map<SharedOperationKey<?>, Object> shared = new ConcurrentHashMap<>();

    public Baker(
            Function<ResourceLocation, UnbakedModel> models,
            Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> sprites) {
        this.models = models;
        this.sprites = sprites;
    }

    @Override
    public ResolvedModel getModel(ResourceLocation id) {
        ResolvedModel model = resolved.get(id);
        if (model != null) return model;
        UnbakedModel unbaked;
        synchronized (this) {
            unbaked = models.apply(id);
            if (unbaked instanceof BlockModel block && block.getParentLocation() != null && block.parent == null)
                unbaked.resolveParents(models);
        }
        model = new ResolvedModel(id, unbaked);
        ResolvedModel previous = resolved.putIfAbsent(id, model);
        return previous != null ? previous : model;
    }

    @Override
    public Materials materials() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T compute(SharedOperationKey<T> key) {
        Object value = shared.get(key);
        if (value != null) return (T) value;
        T computed = key.compute(this);
        Object previous = shared.putIfAbsent(key, computed);
        return previous != null ? (T) previous : computed;
    }

    @Override
    public TextureAtlasSprite sprite(Material material) {
        return sprites.apply(material.vanilla());
    }

    @Override
    public Material.Baked get(Material material, ModelDebugName name) {
        return new Material.Baked(sprite(material), material.forceTranslucent());
    }

    @Override
    public Material.Baked resolveSlot(TextureSlots slots, String slot, ModelDebugName name) {
        Material material = slots.getMaterial(slot);
        return material == null ? reportMissingReference(slot, name) : get(material, name);
    }

    @Override
    public Material.Baked reportMissingReference(String slot, ModelDebugName name) {
        LOGGER.warn("Unable to resolve texture reference: {} in {}", slot, name.debugName());
        return new Material.Baked(sprites.apply(new net.minecraft.client.resources.model.Material(
                TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation())), false);
    }
}
