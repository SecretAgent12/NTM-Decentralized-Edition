// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/** Codecs 26.x has and 1.21.1 lacks. */
public final class Codecs {

    private Codecs() {}

    /** 26.x ByteBufCodecs.LONG: a fixed-width long (1.21.1 only has VAR_LONG). */
    public static final StreamCodec<ByteBuf, Long> LONG = new StreamCodec<>() {
        @Override
        public Long decode(ByteBuf buf) {
            return buf.readLong();
        }

        @Override
        public void encode(ByteBuf buf, Long value) {
            buf.writeLong(value);
        }
    };

    /** 26.x TagKey.streamCodec(registry): the tag's id on the wire. */
    public static <T> StreamCodec<ByteBuf, TagKey<T>> tagStreamCodec(ResourceKey<? extends Registry<T>> registry) {
        return ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(registry, id), TagKey::location);
    }
}
