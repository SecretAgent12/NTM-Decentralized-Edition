// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import java.util.Optional;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * The 26.x CompoundTag / ListTag getters over 1.21.1 tags. In 26.x the plain getters
 * return Optional (present only when the entry exists AND has the right type) and the
 * *Or variants take the default explicitly; in 1.21.1 the getters return the value or a
 * zero/empty default. The backport rewrites `tag.getIntOr(k, d)` into
 * `Nbt.getIntOr(tag, k, d)` and `tag.getInt(k).orElse(d)` into
 * `Nbt.getInt(tag, k).orElse(d)` at the positions javac reports.
 */
public final class Nbt {

    private Nbt() {}

    private static NumericTag num(CompoundTag tag, String key) {
        return tag.get(key) instanceof NumericTag n ? n : null;
    }

    private static NumericTag num(ListTag list, int i) {
        return i >= 0 && i < list.size() && list.get(i) instanceof NumericTag n ? n : null;
    }

    // ---- CompoundTag, Optional getters -------------------------------------------------

    public static Optional<Byte> getByte(CompoundTag tag, String key) {
        NumericTag n = num(tag, key);
        return n == null ? Optional.empty() : Optional.of(n.getAsByte());
    }

    public static Optional<Short> getShort(CompoundTag tag, String key) {
        NumericTag n = num(tag, key);
        return n == null ? Optional.empty() : Optional.of(n.getAsShort());
    }

    public static Optional<Integer> getInt(CompoundTag tag, String key) {
        NumericTag n = num(tag, key);
        return n == null ? Optional.empty() : Optional.of(n.getAsInt());
    }

    public static Optional<Long> getLong(CompoundTag tag, String key) {
        NumericTag n = num(tag, key);
        return n == null ? Optional.empty() : Optional.of(n.getAsLong());
    }

    public static Optional<Float> getFloat(CompoundTag tag, String key) {
        NumericTag n = num(tag, key);
        return n == null ? Optional.empty() : Optional.of(n.getAsFloat());
    }

    public static Optional<Double> getDouble(CompoundTag tag, String key) {
        NumericTag n = num(tag, key);
        return n == null ? Optional.empty() : Optional.of(n.getAsDouble());
    }

    public static Optional<Boolean> getBoolean(CompoundTag tag, String key) {
        NumericTag n = num(tag, key);
        return n == null ? Optional.empty() : Optional.of(n.getAsByte() != 0);
    }

    public static Optional<String> getString(CompoundTag tag, String key) {
        return tag.get(key) instanceof StringTag s ? Optional.of(s.getAsString()) : Optional.empty();
    }

    public static Optional<CompoundTag> getCompound(CompoundTag tag, String key) {
        return tag.get(key) instanceof CompoundTag c ? Optional.of(c) : Optional.empty();
    }

    public static Optional<ListTag> getList(CompoundTag tag, String key) {
        return tag.get(key) instanceof ListTag l ? Optional.of(l) : Optional.empty();
    }

    public static Optional<byte[]> getByteArray(CompoundTag tag, String key) {
        return tag.get(key) instanceof ByteArrayTag a ? Optional.of(a.getAsByteArray()) : Optional.empty();
    }

    public static Optional<int[]> getIntArray(CompoundTag tag, String key) {
        return tag.get(key) instanceof IntArrayTag a ? Optional.of(a.getAsIntArray()) : Optional.empty();
    }

    public static Optional<long[]> getLongArray(CompoundTag tag, String key) {
        return tag.get(key) instanceof LongArrayTag a ? Optional.of(a.getAsLongArray()) : Optional.empty();
    }

    // ---- CompoundTag, *Or getters -------------------------------------------------------

    public static byte getByteOr(CompoundTag tag, String key, byte def) {
        NumericTag n = num(tag, key);
        return n == null ? def : n.getAsByte();
    }

    public static short getShortOr(CompoundTag tag, String key, short def) {
        NumericTag n = num(tag, key);
        return n == null ? def : n.getAsShort();
    }

    public static int getIntOr(CompoundTag tag, String key, int def) {
        NumericTag n = num(tag, key);
        return n == null ? def : n.getAsInt();
    }

    public static long getLongOr(CompoundTag tag, String key, long def) {
        NumericTag n = num(tag, key);
        return n == null ? def : n.getAsLong();
    }

    public static float getFloatOr(CompoundTag tag, String key, float def) {
        NumericTag n = num(tag, key);
        return n == null ? def : n.getAsFloat();
    }

    public static double getDoubleOr(CompoundTag tag, String key, double def) {
        NumericTag n = num(tag, key);
        return n == null ? def : n.getAsDouble();
    }

    public static boolean getBooleanOr(CompoundTag tag, String key, boolean def) {
        NumericTag n = num(tag, key);
        return n == null ? def : n.getAsByte() != 0;
    }

    public static String getStringOr(CompoundTag tag, String key, String def) {
        return tag.get(key) instanceof StringTag s ? s.getAsString() : def;
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.get(key) instanceof CompoundTag c ? c : new CompoundTag();
    }

    public static ListTag getListOrEmpty(CompoundTag tag, String key) {
        return tag.get(key) instanceof ListTag l ? l : new ListTag();
    }

    // ---- ListTag ------------------------------------------------------------------------

    public static Optional<CompoundTag> getCompound(ListTag list, int i) {
        return i >= 0 && i < list.size() && list.get(i) instanceof CompoundTag c ? Optional.of(c) : Optional.empty();
    }

    public static CompoundTag getCompoundOrEmpty(ListTag list, int i) {
        return getCompound(list, i).orElseGet(CompoundTag::new);
    }

    public static Optional<ListTag> getList(ListTag list, int i) {
        return i >= 0 && i < list.size() && list.get(i) instanceof ListTag l ? Optional.of(l) : Optional.empty();
    }

    public static ListTag getListOrEmpty(ListTag list, int i) {
        return getList(list, i).orElseGet(ListTag::new);
    }

    public static Optional<String> getString(ListTag list, int i) {
        return i >= 0 && i < list.size() && list.get(i) instanceof StringTag s ? Optional.of(s.getAsString()) : Optional.empty();
    }

    public static String getStringOr(ListTag list, int i, String def) {
        return getString(list, i).orElse(def);
    }

    public static Optional<Integer> getInt(ListTag list, int i) {
        NumericTag n = num(list, i);
        return n == null ? Optional.empty() : Optional.of(n.getAsInt());
    }

    public static int getIntOr(ListTag list, int i, int def) {
        NumericTag n = num(list, i);
        return n == null ? def : n.getAsInt();
    }

    public static Optional<Double> getDouble(ListTag list, int i) {
        NumericTag n = num(list, i);
        return n == null ? Optional.empty() : Optional.of(n.getAsDouble());
    }

    public static double getDoubleOr(ListTag list, int i, double def) {
        NumericTag n = num(list, i);
        return n == null ? def : n.getAsDouble();
    }

    public static Optional<Float> getFloat(ListTag list, int i) {
        NumericTag n = num(list, i);
        return n == null ? Optional.empty() : Optional.of(n.getAsFloat());
    }

    public static float getFloatOr(ListTag list, int i, float def) {
        NumericTag n = num(list, i);
        return n == null ? def : n.getAsFloat();
    }

    /** 26.x CompoundTag.read(key, codec): decode the entry, empty if absent or invalid. */
    public static <T> Optional<T> read(CompoundTag tag, String key, com.mojang.serialization.Codec<T> codec) {
        Tag value = tag.get(key);
        return value == null ? Optional.empty()
                : codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, value).result();
    }

    /** 26.x CompoundTag.store(key, codec, value). */
    public static <T> void store(CompoundTag tag, String key, com.mojang.serialization.Codec<T> codec, T value) {
        codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, value).result().ifPresent(v -> tag.put(key, v));
    }

    /** 26.x ListTag.compoundStream(): the compound entries, skipping anything else. */
    public static java.util.stream.Stream<CompoundTag> compoundStream(ListTag list) {
        return list.stream().filter(t -> t instanceof CompoundTag).map(t -> (CompoundTag) t);
    }

    /** 26.x Tag.asString(): Optional of the string value (present only for a StringTag). */
    public static Optional<String> asString(Tag tag) {
        return tag instanceof StringTag s ? Optional.of(s.getAsString()) : Optional.empty();
    }
}
