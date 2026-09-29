// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 26.x {@code net.minecraft.client.renderer.item.ModelRenderProperties}: the per-model render
 * settings (gui light, particle, display transforms) an item layer takes from its base model.
 */
public record ModelRenderProperties(
        boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms) {

    /** backport: built from the 1.21.1 baked model instead of a 26.x ResolvedModel. */
    public static ModelRenderProperties fromBakedModel(BakedModel model) {
        return new ModelRenderProperties(
                model.usesBlockLight(), model.getParticleIcon(), model.getTransforms());
    }

    public void applyToLayer(
            ItemStackRenderState.LayerRenderState layer, ItemDisplayContext displayContext) {
        layer.setUsesBlockLight(usesBlockLight);
        layer.setParticleIcon(particleIcon);
        layer.setTransform(transforms.getTransform(displayContext));
    }
}
