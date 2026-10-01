// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 26.x SavedDataType: id + constructor + codec. 1.21.1 identifies saved data by a
 * file name and loads/saves it through SavedData.Factory with CompoundTag hooks.
 *
 * File name: namespace_path (1.21.1 does not create sub-folders for saved data),
 * e.g. hbm:impact_data -> data/hbm_impact_data.dat. The value is stored under
 * "data" in that file. Classes extend SavedDataCompat, which writes it back with
 * the same codec.
 */
public final class SavedDataType<T extends SavedData> {

    // backport-fix: BF-018 — the file name and the factory are built once, not on every lookup.
    // NTM looks some saved data up every tick (TomSaveData per level and per player), and the
    // old record rebuilt the string and a new Factory each time.
    private final ResourceLocation id;
    private final Supplier<T> constructor;
    private final Codec<T> codec;
    private final @Nullable DataFixTypes dataFixType;
    private final String fileName;
    private final SavedData.Factory<T> factory;

    public SavedDataType(
            ResourceLocation id, Supplier<T> constructor, Codec<T> codec, @Nullable DataFixTypes dataFixType) {
        this.id = id;
        this.constructor = constructor;
        this.codec = codec;
        this.dataFixType = dataFixType;
        this.fileName = id.getNamespace() + "_" + id.getPath().replace('/', '_');
        this.factory = new SavedData.Factory<>(
                () -> bind(constructor.get()),
                (tag, lookup) -> bind(codec.parse(lookup.createSerializationContext(NbtOps.INSTANCE), tag.get("data"))
                        .getOrThrow(e -> new IllegalStateException("Failed to load " + id + ": " + e))),
                dataFixType);
    }

    public ResourceLocation id() {
        return id;
    }

    public Supplier<T> constructor() {
        return constructor;
    }

    public Codec<T> codec() {
        return codec;
    }

    public @Nullable DataFixTypes dataFixType() {
        return dataFixType;
    }

    public String fileName() {
        return fileName;
    }

    public SavedData.Factory<T> factory() {
        return factory;
    }

    @SuppressWarnings("unchecked")
    private T bind(T data) {
        ((SavedDataCompat) data).backport$bind((Codec<SavedDataCompat>) (Codec<?>) codec);
        return data;
    }

    /** 26.x DimensionDataStorage.computeIfAbsent(SavedDataType). */
    public static <T extends SavedData> T computeIfAbsent(DimensionDataStorage storage, SavedDataType<T> type) {
        return storage.computeIfAbsent(type.factory, type.fileName);
    }

    /** 26.x DimensionDataStorage.get(SavedDataType). */
    public static <T extends SavedData> @Nullable T get(DimensionDataStorage storage, SavedDataType<T> type) {
        return storage.get(type.factory, type.fileName);
    }
}
