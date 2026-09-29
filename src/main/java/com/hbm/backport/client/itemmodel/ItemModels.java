// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * 26.x {@code net.minecraft.client.renderer.item.ItemModels}: the item model type registry
 * (vanilla types the hbm definitions use + everything {@code ClientRegistry.registerItemModels}
 * adds).
 */
public final class ItemModels {
    public static final IdMapper<MapCodec<? extends ItemModel.Unbaked>> ID_MAPPER = new IdMapper<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<ItemModel.Unbaked> CODEC =
            Codec.lazyInitialized(
                    () ->
                            ID_MAPPER.codec(ResourceLocation.CODEC)
                                    .dispatch("type", ItemModel.Unbaked::type, c -> (MapCodec) c));

    /** For codecs built while this class initializes. */
    public static Codec<ItemModel.Unbaked> lazyCodec() {
        return Codec.lazyInitialized(() -> CODEC);
    }

    static {
        ID_MAPPER.put(mc("empty"), EmptyModel.Unbaked.MAP_CODEC);
        ID_MAPPER.put(mc("model"), BlockModelWrapper.Unbaked.MAP_CODEC);
        ID_MAPPER.put(mc("range_dispatch"), RangeSelectItemModel.Unbaked.MAP_CODEC);
        ID_MAPPER.put(mc("special"), SpecialModelWrapper.Unbaked.MAP_CODEC);
        ID_MAPPER.put(mc("composite"), CompositeModel.Unbaked.MAP_CODEC);
        ID_MAPPER.put(mc("condition"), ConditionalItemModel.Unbaked.MAP_CODEC);
        ID_MAPPER.put(mc("select"), SelectItemModel.Unbaked.MAP_CODEC);
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private ItemModels() {}
}
