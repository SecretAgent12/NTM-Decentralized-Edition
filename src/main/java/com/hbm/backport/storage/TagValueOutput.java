// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.storage;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** ValueOutput over a CompoundTag. See ValueOutput. */
public final class TagValueOutput implements ValueOutput {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final CompoundTag tag;
    private final HolderLookup.Provider lookup;
    private DynamicOps<Tag> ops;

    private TagValueOutput(CompoundTag tag, HolderLookup.Provider lookup) {
        this.tag = tag;
        this.lookup = lookup;
    }

    /** Writes straight into an existing tag -- what the 1.21.1 bridges hand out. */
    public static ValueOutput wrap(CompoundTag tag, HolderLookup.Provider lookup) {
        return new TagValueOutput(tag, lookup);
    }

    /** 26.x's factory; the problem reporter has no 1.21.1 counterpart and is unused. */
    public static TagValueOutput createWithContext(Object reporter, HolderLookup.Provider lookup) {
        return new TagValueOutput(new CompoundTag(), lookup);
    }

    public CompoundTag buildResult() {
        return tag;
    }

    public static CompoundTag tagOf(ValueOutput output) {
        return ((TagValueOutput) output).tag;
    }

    public static HolderLookup.Provider lookupOf(ValueOutput output) {
        return ((TagValueOutput) output).lookup;
    }

    private DynamicOps<Tag> ops() {
        if (ops == null) ops = lookup.createSerializationContext(NbtOps.INSTANCE);
        return ops;
    }

    @Override
    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(ops(), value).resultOrPartial(e -> LOGGER.warn("Failed to store {}: {}", key, e)).ifPresent(t -> tag.put(key, t));
    }

    @Override
    public <T> void storeNullable(String key, Codec<T> codec, @Nullable T value) {
        if (value != null) store(key, codec, value);
    }

    @Override
    public <T> void store(MapCodec<T> codec, T value) {
        codec.codec().encodeStart(ops(), value).resultOrPartial(e -> LOGGER.warn("Failed to store map value: {}", e))
                .ifPresent(t -> { if (t instanceof CompoundTag c) tag.merge(c); });
    }

    @Override public void putBoolean(String key, boolean value) { tag.putBoolean(key, value); }
    @Override public void putByte(String key, byte value) { tag.putByte(key, value); }
    @Override public void putShort(String key, short value) { tag.putShort(key, value); }
    @Override public void putInt(String key, int value) { tag.putInt(key, value); }
    @Override public void putLong(String key, long value) { tag.putLong(key, value); }
    @Override public void putFloat(String key, float value) { tag.putFloat(key, value); }
    @Override public void putDouble(String key, double value) { tag.putDouble(key, value); }
    @Override public void putString(String key, String value) { tag.putString(key, value); }
    @Override public void putIntArray(String key, int[] value) { tag.putIntArray(key, value); }

    @Override
    public ValueOutput child(String key) {
        CompoundTag child = new CompoundTag();
        tag.put(key, child);
        return new TagValueOutput(child, lookup);
    }

    @Override
    public ValueOutputList childrenList(String key) {
        ListTag list = new ListTag();
        tag.put(key, list);
        return new ValueOutputList() {
            @Override
            public ValueOutput addChild() {
                CompoundTag child = new CompoundTag();
                list.add(child);
                return new TagValueOutput(child, lookup);
            }

            @Override public void discardLast() { list.remove(list.size() - 1); }
            @Override public boolean isEmpty() { return list.isEmpty(); }
        };
    }

    @Override
    public <T> TypedOutputList<T> list(String key, Codec<T> codec) {
        ListTag list = new ListTag();
        tag.put(key, list);
        return new TypedOutputList<>() {
            @Override
            public void add(T value) {
                codec.encodeStart(ops(), value).resultOrPartial(e -> LOGGER.warn("Failed to store list element: {}", e)).ifPresent(list::add);
            }

            @Override public boolean isEmpty() { return list.isEmpty(); }
        };
    }

    @Override public void discard(String key) { tag.remove(key); }
    @Override public boolean isEmpty() { return tag.isEmpty(); }
}
