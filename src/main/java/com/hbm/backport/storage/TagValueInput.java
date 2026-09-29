// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.storage;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.IntArrayTag;
import org.slf4j.Logger;

/** ValueInput over a CompoundTag. See ValueInput. */
public final class TagValueInput implements ValueInput {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final CompoundTag tag;
    private final HolderLookup.Provider lookup;
    private DynamicOps<Tag> ops;

    private TagValueInput(CompoundTag tag, HolderLookup.Provider lookup) {
        this.tag = tag;
        this.lookup = lookup;
    }

    public static ValueInput of(CompoundTag tag, HolderLookup.Provider lookup) {
        return new TagValueInput(tag, lookup);
    }

    /** 26.x's factory; the problem reporter has no 1.21.1 counterpart and is unused. */
    public static ValueInput create(Object reporter, HolderLookup.Provider lookup, CompoundTag tag) {
        return new TagValueInput(tag, lookup);
    }

    /** The CompoundTag behind a ValueInput this layer created -- for the bridges back to 1.21.1. */
    public static CompoundTag tagOf(ValueInput input) {
        return ((TagValueInput) input).tag;
    }

    private DynamicOps<Tag> ops() {
        if (ops == null) ops = lookup.createSerializationContext(NbtOps.INSTANCE);
        return ops;
    }

    @Override
    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag value = tag.get(key);
        if (value == null) return Optional.empty();
        return codec.parse(ops(), value).resultOrPartial(e -> LOGGER.warn("Failed to read {}: {}", key, e));
    }

    @Override
    public <T> Optional<T> read(MapCodec<T> codec) {
        return codec.codec().parse(ops(), tag).resultOrPartial(e -> LOGGER.warn("Failed to read map value: {}", e));
    }

    @Override
    public Optional<ValueInput> child(String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? Optional.of(new TagValueInput(tag.getCompound(key), lookup)) : Optional.empty();
    }

    @Override
    public ValueInput childOrEmpty(String key) {
        return new TagValueInput(tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : new CompoundTag(), lookup);
    }

    @Override
    public Optional<ValueInputList> childrenList(String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) return Optional.empty();
        return Optional.of(children(tag.getList(key, Tag.TAG_COMPOUND)));
    }

    @Override
    public ValueInputList childrenListOrEmpty(String key) {
        return children(tag.contains(key, Tag.TAG_LIST) ? tag.getList(key, Tag.TAG_COMPOUND) : new ListTag());
    }

    private ValueInputList children(ListTag list) {
        List<ValueInput> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) out.add(new TagValueInput(list.getCompound(i), lookup));
        return new ValueInputList() {
            @Override public boolean isEmpty() { return out.isEmpty(); }
            @Override public Stream<ValueInput> stream() { return out.stream(); }
            @Override public Iterator<ValueInput> iterator() { return out.iterator(); }
        };
    }

    @Override
    public <T> Optional<TypedInputList<T>> list(String key, Codec<T> codec) {
        Tag raw = tag.get(key);
        if (!(raw instanceof ListTag list)) return Optional.empty();
        return Optional.of(typed(list, codec));
    }

    @Override
    public <T> TypedInputList<T> listOrEmpty(String key, Codec<T> codec) {
        return tag.get(key) instanceof ListTag list ? typed(list, codec) : typed(new ListTag(), codec);
    }

    private <T> TypedInputList<T> typed(ListTag list, Codec<T> codec) {
        List<T> out = new ArrayList<>(list.size());
        for (Tag element : list) codec.parse(ops(), element).resultOrPartial(e -> LOGGER.warn("Failed to read list element: {}", e)).ifPresent(out::add);
        return new TypedInputList<>() {
            @Override public boolean isEmpty() { return out.isEmpty(); }
            @Override public Stream<T> stream() { return out.stream(); }
            @Override public Iterator<T> iterator() { return out.iterator(); }
        };
    }

    private NumericTag number(String key) {
        return tag.get(key) instanceof NumericTag n ? n : null;
    }

    @Override public boolean getBooleanOr(String key, boolean fallback) { NumericTag n = number(key); return n == null ? fallback : n.getAsByte() != 0; }
    @Override public byte getByteOr(String key, byte fallback) { NumericTag n = number(key); return n == null ? fallback : n.getAsByte(); }
    @Override public int getShortOr(String key, short fallback) { NumericTag n = number(key); return n == null ? fallback : n.getAsShort(); }
    @Override public Optional<Integer> getInt(String key) { NumericTag n = number(key); return n == null ? Optional.empty() : Optional.of(n.getAsInt()); }
    @Override public int getIntOr(String key, int fallback) { NumericTag n = number(key); return n == null ? fallback : n.getAsInt(); }
    @Override public long getLongOr(String key, long fallback) { NumericTag n = number(key); return n == null ? fallback : n.getAsLong(); }
    @Override public Optional<Long> getLong(String key) { NumericTag n = number(key); return n == null ? Optional.empty() : Optional.of(n.getAsLong()); }
    @Override public float getFloatOr(String key, float fallback) { NumericTag n = number(key); return n == null ? fallback : n.getAsFloat(); }
    @Override public double getDoubleOr(String key, double fallback) { NumericTag n = number(key); return n == null ? fallback : n.getAsDouble(); }

    @Override
    public Optional<String> getString(String key) {
        return tag.get(key) instanceof StringTag s ? Optional.of(s.getAsString()) : Optional.empty();
    }

    @Override
    public String getStringOr(String key, String fallback) {
        return tag.get(key) instanceof StringTag s ? s.getAsString() : fallback;
    }

    @Override
    public Optional<int[]> getIntArray(String key) {
        return tag.get(key) instanceof IntArrayTag a ? Optional.of(a.getAsIntArray()) : Optional.empty();
    }

    @Override
    public HolderLookup.Provider lookup() {
        return lookup;
    }
}
