// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.item.CompositeModel}: all sub-models' layers. */
public final class CompositeModel implements ItemModel {
    private final List<ItemModel> models;

    public CompositeModel(List<ItemModel> models) {
        this.models = models;
    }

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed) {
        output.appendModelIdentityElement(this);
        output.ensureCapacity(models.size());
        for (ItemModel model : models)
            model.update(output, item, resolver, displayContext, level, owner, seed);
    }

    public record Unbaked(List<ItemModel.Unbaked> models) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i -> i.group(ItemModels.lazyCodec().listOf().fieldOf("models").forGetter(Unbaked::models))
                                .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (ItemModel.Unbaked model : models) model.resolveDependencies(resolver);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new CompositeModel(models.stream().map(m -> m.bake(context, transformation)).toList());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
