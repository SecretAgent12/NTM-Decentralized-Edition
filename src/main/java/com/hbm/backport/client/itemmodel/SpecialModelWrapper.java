// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.google.common.base.Suppliers;
import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** 26.x {@code minecraft:special}: a special renderer with render properties of a base model. */
public class SpecialModelWrapper<T> implements ItemModel {
    private final SpecialModelRenderer<T> renderer;
    private final ModelRenderProperties properties;
    private final @Nullable Matrix4fc transformation;
    private final Supplier<Vector3fc[]> extents;

    public SpecialModelWrapper(SpecialModelRenderer<T> renderer, ModelRenderProperties properties, Matrix4fc transformation) {
        this.renderer = renderer;
        this.properties = properties;
        this.transformation =
                (transformation.properties() & Matrix4fc.PROPERTY_IDENTITY) != 0 ? null : transformation;
        this.extents =
                Suppliers.memoize(
                        () -> {
                            Set<Vector3fc> points = new HashSet<>();
                            renderer.getExtents(points::add);
                            return points.toArray(new Vector3fc[0]);
                        });
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
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            output.setAnimated();
            output.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
        }
        T argument = renderer.extractArgument(item);
        layer.setExtents(extents);
        layer.setLocalTransform(transformation);
        layer.setupSpecialModel(renderer, argument);
        if (argument != null) output.appendModelIdentityElement(argument);
        properties.applyToLayer(layer, displayContext);
    }

    public record Unbaked(ResourceLocation base, SpecialModelRenderer.Unbaked<?> specialModel)
            implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                ResourceLocation.CODEC.fieldOf("base").forGetter(Unbaked::base),
                                                SpecialModelRenderers.CODEC.fieldOf("model").forGetter(Unbaked::specialModel))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            SpecialModelRenderer<?> renderer = specialModel.bake(context);
            if (renderer == null) return context.missingItemModel();
            return new SpecialModelWrapper<>(
                    renderer, ModelRenderProperties.fromBakedModel(context.bakedModel(base)), transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
