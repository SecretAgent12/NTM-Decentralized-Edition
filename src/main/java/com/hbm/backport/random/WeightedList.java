// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.random;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

/**
 * 26.x net.minecraft.util.random.WeightedList (1.21.5+): an immutable list of
 * Weighted entries with weighted random selection. Serialised as the list of
 * {data, weight} entries, as in 26.x.
 */
public final class WeightedList<E> {

    private final List<Weighted<E>> items;
    private final int totalWeight;

    private WeightedList(List<? extends Weighted<E>> items) {
        this.items = List.copyOf(items);
        long total = 0;
        for (Weighted<E> w : this.items) total += w.weight();
        if (total > Integer.MAX_VALUE) throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
        this.totalWeight = (int) total;
    }

    public static <E> WeightedList<E> of() {
        return new WeightedList<>(List.of());
    }

    public static <E> WeightedList<E> of(E value) {
        return new WeightedList<>(List.of(new Weighted<>(value, 1)));
    }

    @SafeVarargs
    public static <E> WeightedList<E> of(Weighted<E>... items) {
        return new WeightedList<>(List.of(items));
    }

    public static <E> WeightedList<E> of(List<Weighted<E>> items) {
        return new WeightedList<>(items);
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public <T> WeightedList<T> map(Function<E, T> f) {
        return new WeightedList<>(items.stream().map(w -> w.map(f)).toList());
    }

    public Optional<E> getRandom(RandomSource random) {
        if (totalWeight == 0) return Optional.empty();
        int pick = random.nextInt(totalWeight);
        for (Weighted<E> w : items) {
            pick -= w.weight();
            if (pick < 0) return Optional.of(w.value());
        }
        return Optional.empty();
    }

    public E getRandomOrThrow(RandomSource random) {
        return getRandom(random).orElseThrow(() -> new IllegalStateException("Weighted list has no elements"));
    }

    public List<Weighted<E>> unwrap() {
        return items;
    }

    public boolean contains(E value) {
        for (Weighted<E> w : items) if (w.value().equals(value)) return true;
        return false;
    }

    public static <E> Codec<WeightedList<E>> codec(Codec<E> element) {
        return Weighted.codec(element).listOf().xmap(WeightedList::of, WeightedList::unwrap);
    }

    public static <E> Codec<WeightedList<E>> nonEmptyCodec(Codec<E> element) {
        return ExtraCodecs.nonEmptyList(Weighted.codec(element).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
    }

    public static <B extends ByteBuf, E> StreamCodec<B, WeightedList<E>> streamCodec(StreamCodec<B, E> element) {
        return Weighted.streamCodec(element).apply(ByteBufCodecs.list()).map(WeightedList::of, WeightedList::unwrap);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof WeightedList<?> l && items.equals(l.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    public static final class Builder<E> {
        private final ImmutableList.Builder<Weighted<E>> result = ImmutableList.builder();

        public Builder<E> add(E value) {
            return add(value, 1);
        }

        public Builder<E> add(E value, int weight) {
            result.add(new Weighted<>(value, weight));
            return this;
        }

        public WeightedList<E> build() {
            return new WeightedList<>(result.build());
        }
    }
}
