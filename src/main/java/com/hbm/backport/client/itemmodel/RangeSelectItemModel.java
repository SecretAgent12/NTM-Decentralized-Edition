// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** 26.x {@code minecraft:range_dispatch}: the entry with the highest threshold <= value. */
public class RangeSelectItemModel implements ItemModel {
    private final RangeSelectItemModelProperty property;
    private final float scale;
    private final float[] thresholds;
    private final ItemModel[] models;
    private final ItemModel fallback;

    RangeSelectItemModel(
            RangeSelectItemModelProperty property, float scale, float[] thresholds, ItemModel[] models, ItemModel fallback) {
        this.property = property;
        this.scale = scale;
        this.thresholds = thresholds;
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
        float value = property.get(item, level, owner, seed) * scale;
        ItemModel model = fallback;
        if (!Float.isNaN(value)) {
            for (int i = thresholds.length - 1; i >= 0; i--) {
                if (thresholds[i] <= value) {
                    model = models[i];
                    break;
                }
            }
        }
        model.update(output, item, resolver, displayContext, level, owner, seed);
    }

    public record Entry(float threshold, ItemModel.Unbaked model) {
        public static final Codec<Entry> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.FLOAT.fieldOf("threshold").forGetter(Entry::threshold),
                                                ItemModels.lazyCodec().fieldOf("model").forGetter(Entry::model))
                                        .apply(i, Entry::new));
    }

    public record Unbaked(
            RangeSelectItemModelProperty property, float scale, List<Entry> entries, Optional<ItemModel.Unbaked> fallback)
            implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                RangeSelectItemModelProperties.MAP_CODEC.forGetter(Unbaked::property),
                                                Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(Unbaked::scale),
                                                Entry.CODEC.listOf().fieldOf("entries").forGetter(Unbaked::entries),
                                                ItemModels.lazyCodec().optionalFieldOf("fallback").forGetter(Unbaked::fallback))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (Entry e : entries) e.model().resolveDependencies(resolver);
            fallback.ifPresent(f -> f.resolveDependencies(resolver));
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            List<Entry> sorted = entries.stream().sorted(Comparator.comparingDouble(Entry::threshold)).toList();
            float[] thresholds = new float[sorted.size()];
            ItemModel[] models = new ItemModel[sorted.size()];
            for (int i = 0; i < thresholds.length; i++) {
                thresholds[i] = sorted.get(i).threshold();
                models[i] = sorted.get(i).model().bake(context, transformation);
            }
            ItemModel fb = fallback.map(f -> f.bake(context, transformation)).orElse(context.missingItemModel());
            return new RangeSelectItemModel(property, scale, thresholds, models, fb);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
