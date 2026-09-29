// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code minecraft:model} ({@code BlockModelWrapper}/{@code CuboidItemModelWrapper}): the quads
 * of one model plus tint sources. backport: the model is the 1.21.1 baked model (loaded standalone);
 * its quads are split per 1.21.1 item render type into layers (26.x keeps the type per quad).
 */
public class BlockModelWrapper implements ItemModel {
    private final BakedModel model;
    private final List<ItemTintSource> tints;
    private final ModelRenderProperties properties;
    private final @Nullable Matrix4fc transformation;

    public BlockModelWrapper(BakedModel model, List<ItemTintSource> tints, Matrix4fc transformation) {
        this.model = model;
        this.tints = tints;
        this.properties = ModelRenderProperties.fromBakedModel(model);
        this.transformation =
                (transformation.properties() & Matrix4fc.PROPERTY_IDENTITY) != 0 ? null : transformation;
    }

    public BakedModel bakedModel() {
        return model;
    }

    public List<ItemTintSource> tints() {
        return tints;
    }

    static boolean hasSpecialAnimatedTexture(ItemStack stack) {
        return stack.is(ItemTags.COMPASSES) || stack.is(Items.CLOCK);
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
        int[] tintValues = new int[tints.size()];
        for (int i = 0; i < tintValues.length; i++) {
            tintValues[i] = tints.get(i).calculate(item, level, living);
            output.appendModelIdentityElement(tintValues[i]);
        }
        ItemStackRenderState.FoilType foil = ItemStackRenderState.FoilType.NONE;
        if (item.hasFoil()) {
            foil =
                    hasSpecialAnimatedTexture(item)
                            ? ItemStackRenderState.FoilType.SPECIAL
                            : ItemStackRenderState.FoilType.STANDARD;
            output.setAnimated();
            output.appendModelIdentityElement(foil);
        }
        appendLayers(output, item, displayContext, model, properties, tintValues, foil, transformation);
    }

    /** Layers for a 1.21.1 baked model: one per (render pass, item render type). */
    static void appendLayers(
            ItemStackRenderState output,
            ItemStack item,
            ItemDisplayContext displayContext,
            BakedModel model,
            ModelRenderProperties properties,
            int[] tintValues,
            ItemStackRenderState.FoilType foil,
            @Nullable Matrix4fc transformation) {
        boolean fabulous = Minecraft.useShaderTransparency();
        for (BakedModel pass : model.getRenderPasses(item, fabulous)) {
            for (RenderType type : pass.getRenderTypes(item, fabulous)) {
                ItemQuads.Converted converted = ItemQuads.convert(pass, type);
                ItemStackRenderState.LayerRenderState layer = output.newLayer();
                layer.prepareQuadList().addAll(converted.quads());
                for (int tint : tintValues) layer.tintLayers().add(tint);
                layer.setFoilType(foil);
                layer.setExtents(converted.extents());
                layer.setLocalTransform(transformation);
                properties.applyToLayer(layer, displayContext);
                if (converted.animated()) output.setAnimated();
            }
        }
    }

    public record Unbaked(ResourceLocation model, List<ItemTintSource> tints)
            implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                ResourceLocation.CODEC.fieldOf("model").forGetter(Unbaked::model),
                                                ItemTintSources.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("tints", List.of())
                                                        .forGetter(Unbaked::tints))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new BlockModelWrapper(context.bakedModel(model), tints, transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
