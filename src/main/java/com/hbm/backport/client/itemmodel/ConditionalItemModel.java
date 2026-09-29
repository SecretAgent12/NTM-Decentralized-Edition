// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** 26.x {@code minecraft:condition}. */
public class ConditionalItemModel implements ItemModel {
    private final ConditionalItemModelProperty property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;

    public ConditionalItemModel(ConditionalItemModelProperty property, ItemModel onTrue, ItemModel onFalse) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
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
        boolean value =
                property.get(item, level, owner != null ? owner.asLivingEntity() : null, seed, displayContext);
        (value ? onTrue : onFalse).update(output, item, resolver, displayContext, level, owner, seed);
    }

    public record Unbaked(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse)
            implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                ConditionalItemModelProperties.MAP_CODEC.forGetter(Unbaked::property),
                                                ItemModels.lazyCodec().fieldOf("on_true").forGetter(Unbaked::onTrue),
                                                ItemModels.lazyCodec().fieldOf("on_false").forGetter(Unbaked::onFalse))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            onTrue.resolveDependencies(resolver);
            onFalse.resolveDependencies(resolver);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new ConditionalItemModel(
                    property, onTrue.bake(context, transformation), onFalse.bake(context, transformation));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
