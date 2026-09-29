// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/** 26.x net.minecraft.util.random.Weighted: a value with a non-negative weight ({data, weight}). */
public record Weighted<T>(T value, int weight) {

    public Weighted {
        if (weight < 0) throw new IllegalArgumentException("Weight should be >= 0");
    }

    public static <E> Codec<Weighted<E>> codec(Codec<E> element) {
        return codec(element.fieldOf("data"));
    }

    public static <E> Codec<Weighted<E>> codec(com.mojang.serialization.MapCodec<E> element) {
        return RecordCodecBuilder.create(i -> i.group(
                element.forGetter(Weighted::value),
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("weight").forGetter(Weighted::weight))
                .apply(i, Weighted::new));
    }

    public static <B extends ByteBuf, T> StreamCodec<B, Weighted<T>> streamCodec(StreamCodec<B, T> element) {
        return StreamCodec.composite(element, Weighted::value, ByteBufCodecs.VAR_INT, Weighted::weight, Weighted::new);
    }

    public <U> Weighted<U> map(Function<T, U> f) {
        return new Weighted<>(f.apply(value), weight);
    }
}
