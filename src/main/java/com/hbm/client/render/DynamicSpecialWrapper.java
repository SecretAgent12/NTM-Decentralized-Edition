// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.google.common.base.Suppliers;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import com.hbm.backport.client.itemmodel.ItemModel;
import com.hbm.backport.client.itemmodel.ItemModelResolver;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import com.hbm.backport.client.itemmodel.ModelRenderProperties;
import com.hbm.backport.client.itemmodel.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import com.hbm.backport.client.itemmodel.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class DynamicSpecialWrapper<T> implements ItemModel {

    private final SpecialModelRenderer<T> renderer;
    private final ArgumentExtractor<T> argument;
    private final @Nullable ArgumentPose<T> argumentPose;
    private final boolean animated;
    private final PropertiesFor properties;
    private final Supplier<Vector3fc[]> extents;
    private final Map<ItemDisplayContext, Matrix4f> poses;

    public DynamicSpecialWrapper(
            SpecialModelRenderer<T> renderer,
            ArgumentExtractor<T> argument,
            boolean animated,
            ModelRenderProperties properties,
            Map<ItemDisplayContext, Matrix4f> poses) {
        this(renderer, argument, null, animated, properties, poses);
    }

    public DynamicSpecialWrapper(
            SpecialModelRenderer<T> renderer,
            ArgumentExtractor<T> argument,
            boolean animated,
            ModelRenderProperties properties) {
        this(renderer, argument, null, animated, properties, Map.of());
    }

    public DynamicSpecialWrapper(
            SpecialModelRenderer<T> renderer,
            ArgumentExtractor<T> argument,
            @Nullable ArgumentPose<T> argumentPose,
            boolean animated,
            ModelRenderProperties properties,
            Map<ItemDisplayContext, Matrix4f> poses) {
        this(renderer, argument, argumentPose, animated, stack -> properties, poses);
    }

    public DynamicSpecialWrapper(
            SpecialModelRenderer<T> renderer,
            ArgumentExtractor<T> argument,
            @Nullable ArgumentPose<T> argumentPose,
            boolean animated,
            PropertiesFor properties,
            Map<ItemDisplayContext, Matrix4f> poses) {
        this.renderer = renderer;
        this.argument = argument;
        this.argumentPose = argumentPose;
        this.animated = animated;
        this.properties = properties;
        this.poses = poses;
        this.extents =
                Suppliers.memoize(
                        () -> {
                            Set<Vector3fc> results = new HashSet<>();
                            renderer.getExtents(results::add);
                            return results.toArray(new Vector3fc[0]);
                        });
    }

    public static ModelRenderProperties properties(
            ItemModel.BakingContext context, ResourceLocation base) {
        // backport: from the 1.21.1 baked base model (26.x: ResolvedModel + texture slots)
        return ModelRenderProperties.fromBakedModel(context.bakedModel(base));
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
            ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foilType);
            output.setAnimated();
            output.appendModelIdentityElement(foilType);
        }

        layer.setExtents(extents);
        T arg = argument.extract(item, displayContext, owner);

        Matrix4f pose = poses.get(displayContext);
        Matrix4fc local =
                arg != null && argumentPose != null
                        ? argumentPose.apply(arg, displayContext, pose)
                        : pose;
        if (local != null) layer.setLocalTransform(local);
        layer.setupSpecialModel(renderer, arg);
        if (animated) output.setAnimated();

        if (arg != null) output.appendModelIdentityElement(arg);
        properties.of(item).applyToLayer(layer, displayContext);
    }

    public SpecialModelRenderer<T> renderer() {
        return renderer;
    }

    @FunctionalInterface
    public interface ArgumentExtractor<T> {
        @Nullable T extract(ItemStack stack, ItemDisplayContext context, @Nullable ItemOwner owner);
    }

    @FunctionalInterface
    public interface ArgumentPose<T> {
        Matrix4fc apply(T argument, ItemDisplayContext context, Matrix4fc fallback);
    }

    @FunctionalInterface
    public interface PropertiesFor {
        ModelRenderProperties of(ItemStack stack);
    }
}
