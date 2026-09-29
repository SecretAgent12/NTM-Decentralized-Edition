// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.item.ItemModel}: a baked node of an item definition
 * ({@code assets/<ns>/items/<id>.json}) that fills an {@link ItemStackRenderState} for a stack.
 * In 1.21.1 the definitions are loaded and baked by {@link ClientItems}; the item's inventory model
 * is replaced by a custom-renderer model and drawn by {@link ItemDefinitionRenderer} (a BEWLR).
 */
public interface ItemModel {
    void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed);

    interface Unbaked extends ResolvableModel {
        MapCodec<? extends Unbaked> type();

        ItemModel bake(BakingContext context, Matrix4fc transformation);
    }

    /**
     * 26.x {@code ItemModel.BakingContext}. backport: 1.21.1 has no item-side ModelBaker/ResolvedModel;
     * the context hands out the 1.21.1 {@link BakedModel} of every model an item definition marked as
     * dependency (loaded as standalone models by {@link ClientItems}).
     */
    final class BakingContext {
        private final Function<ResourceLocation, BakedModel> models;
        private final BakedModel missingModel;
        private final ItemModel missingItemModel;

        public BakingContext(
                Function<ResourceLocation, BakedModel> models,
                BakedModel missingModel,
                ItemModel missingItemModel) {
            this.models = models;
            this.missingModel = missingModel;
            this.missingItemModel = missingItemModel;
        }

        /** backport: the baked 1.21.1 model for a dependency id (the missing model if absent). */
        public BakedModel bakedModel(ResourceLocation id) {
            BakedModel model = models.apply(id);
            return model != null ? model : missingModel;
        }

        public ItemModel missingItemModel() {
            return missingItemModel;
        }

        public EntityModelSet entityModelSet() {
            return Minecraft.getInstance().getEntityModels();
        }
    }
}
