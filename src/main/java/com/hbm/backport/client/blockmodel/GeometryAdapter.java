// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

/**
 * A 26.x {@link UnbakedGeometry} (NeoForge 26 UnbakedModelLoader geometry) as a 1.21.1 NeoForge
 * IUnbakedGeometry: the model JSON's textures/parent/transforms are read by 1.21.1's
 * BlockModel deserializer, the loader-specific keys by the 26.x reader.
 */
public record GeometryAdapter(UnbakedGeometry geometry) implements IUnbakedGeometry<GeometryAdapter> {

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            net.minecraft.client.resources.model.ModelBaker vanillaBaker,
            Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> spriteGetter,
            net.minecraft.client.resources.model.ModelState modelState,
            ItemOverrides overrides) {
        Baker baker = new Baker(vanillaBaker::getModel, spriteGetter);
        TextureSlots slots = TextureSlots.of(context);
        ModelDebugName name = context::getModelName;
        QuadCollection quads = geometry.bake(slots, baker, ModelState.of(modelState), name);
        Material.Baked particle = baker.resolveSlot(slots, "particle", name);
        return new BlockModelAdapter(
                new SingleVariant(new SimpleModelWrapper(quads, context.useAmbientOcclusion(), particle)),
                context.getTransforms(),
                context.isGui3d(),
                context.useBlockLight(),
                overrides);
    }

    /** A geometry loader reading the 26.x loader-specific JSON. */
    public record Loader(Function<JsonObject, UnbakedGeometry> reader) implements IGeometryLoader<GeometryAdapter> {
        @Override
        public GeometryAdapter read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            return new GeometryAdapter(reader.apply(json));
        }
    }
}
