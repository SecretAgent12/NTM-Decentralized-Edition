// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * 26.x net.minecraft.client.resources.model.ModelBaker: resolves models by id, resolves texture
 * materials to sprites and shares expensive bake results between block states
 * ({@link #compute}). Implemented by {@link Baker} over the 1.21.1 model bakery.
 */
public interface ModelBaker {

    ResolvedModel getModel(ResourceLocation id);

    Materials materials();

    default Materials sprites() {
        return materials();
    }

    <T> T compute(SharedOperationKey<T> key);

    /** A bake result shared by every caller passing an equal key during one reload. */
    @FunctionalInterface
    interface SharedOperationKey<T> {
        T compute(ModelBaker baker);
    }

    /** 26.x ModelBaker#materials() (MaterialBaker / SpriteGetter). */
    interface Materials {
        Material.Baked get(Material material, ModelDebugName name);

        Material.Baked resolveSlot(TextureSlots slots, String slot, ModelDebugName name);

        Material.Baked reportMissingReference(String slot, ModelDebugName name);

        TextureAtlasSprite sprite(Material material);
    }
}
