// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;

/** SavedData saved through the codec of its SavedDataType (see there). */
public abstract class SavedDataCompat extends SavedData {

    private Codec<SavedDataCompat> backport$codec;

    final void backport$bind(Codec<SavedDataCompat> codec) {
        this.backport$codec = codec;
    }

    @Override
    public final CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        if (backport$codec == null) {
            throw new IllegalStateException(getClass().getName() + " was not created through its SavedDataType");
        }
        tag.put("data", backport$codec.encodeStart(lookup.createSerializationContext(NbtOps.INSTANCE), this)
                .getOrThrow(e -> new IllegalStateException("Failed to save " + getClass().getName() + ": " + e)));
        return tag;
    }
}
