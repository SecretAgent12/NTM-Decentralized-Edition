// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.util.datafix.HbmDataFixers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Runs NTM's own chunk-data fixers after vanilla's chunk datafixing.
 *
 * <p>backport: 26.x upgrades chunk tags in SimpleRegionStorage.upgradeChunkTag (with
 * dataFixType == CHUNK). In 1.21.1 SimpleRegionStorage only serves entity and POI storage;
 * chunk tags are upgraded by ChunkStorage.upgradeChunkTag (ChunkMap extends ChunkStorage),
 * so this mixin targets that method instead (class name kept for the mixin config).
 */
@Mixin(ChunkStorage.class)
public abstract class MixinSimpleRegionStorage {

    @ModifyReturnValue(
            method =
                    "upgradeChunkTag(Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Supplier;Lnet/minecraft/nbt/CompoundTag;Ljava/util/Optional;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"))
    private CompoundTag hbm$upgradeNtmData(CompoundTag chunkTag) {
        return HbmDataFixers.upgradeChunk(chunkTag);
    }
}
