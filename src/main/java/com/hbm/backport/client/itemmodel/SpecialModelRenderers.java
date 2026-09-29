// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

/** 26.x {@code net.minecraft.client.renderer.special.SpecialModelRenderers}: the type registry. */
public final class SpecialModelRenderers {
    public static final IdMapper<MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> ID_MAPPER =
            new IdMapper<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<SpecialModelRenderer.Unbaked<?>> CODEC =
            Codec.lazyInitialized(
                    () ->
                            (Codec)
                                    ID_MAPPER.codec(ResourceLocation.CODEC)
                                            .dispatch(
                                                    "type",
                                                    u -> (MapCodec) ((SpecialModelRenderer.Unbaked) u).type(),
                                                    c -> (MapCodec) c));

    private SpecialModelRenderers() {}
}
