// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** 26.x {@code minecraft:select}: picks a sub-model by a property value. */
public class SelectItemModel<T> implements ItemModel {
    private final SelectItemModelProperty<T> property;
    private final Map<T, ItemModel> models;
    private final ItemModel fallback;

    public SelectItemModel(SelectItemModelProperty<T> property, Map<T, ItemModel> models, ItemModel fallback) {
        this.property = property;
        this.models = models;
        this.fallback = fallback;
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
        LivingEntity living = owner != null ? owner.asLivingEntity() : null;
        T value = property.get(item, level, living, seed, displayContext);
        ItemModel model = value == null ? null : models.get(value);
        (model != null ? model : fallback)
                .update(output, item, resolver, displayContext, level, owner, seed);
    }

    public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {
        public static <T> Codec<SwitchCase<T>> codec(Codec<T> valueCodec) {
            Codec<List<T>> compact =
                    Codec.either(valueCodec.listOf(), valueCodec)
                            .xmap(
                                    e -> e.map(l -> l, List::of),
                                    l -> l.size() == 1 ? Either.right(l.get(0)) : Either.left(l));
            Codec<List<T>> nonEmpty =
                    compact.validate(
                            l -> l.isEmpty() ? DataResult.error(() -> "Empty case list") : DataResult.success(l));
            return RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            nonEmpty.fieldOf("when").forGetter(SwitchCase::values),
                                            ItemModels.lazyCodec().fieldOf("model").forGetter(SwitchCase::model))
                                    .apply(i, SwitchCase::new));
        }
    }

    public record UnbakedSwitch<P extends SelectItemModelProperty<T>, T>(P property, List<SwitchCase<T>> cases) {
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation, ItemModel fallback) {
            Map<T, ItemModel> map = new HashMap<>();
            for (SwitchCase<T> c : cases) {
                ItemModel model = c.model().bake(context, transformation);
                for (T value : c.values()) map.put(value, model);
            }
            return new SelectItemModel<>(property, map, fallback);
        }

        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (SwitchCase<T> c : cases) c.model().resolveDependencies(resolver);
        }
    }

    public record Unbaked(UnbakedSwitch<?, ?> unbakedSwitch, Optional<ItemModel.Unbaked> fallback)
            implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                SelectItemModelProperties.CODEC.forGetter(Unbaked::unbakedSwitch),
                                                ItemModels.lazyCodec().optionalFieldOf("fallback").forGetter(Unbaked::fallback))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            unbakedSwitch.resolveDependencies(resolver);
            fallback.ifPresent(f -> f.resolveDependencies(resolver));
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ItemModel fb =
                    fallback.map(f -> f.bake(context, transformation)).orElse(context.missingItemModel());
            return unbakedSwitch.bake(context, transformation, fb);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
