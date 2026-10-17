// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.Nbt;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.entity.item.PendingFallers;
import com.hbm.interfaces.injected.IChunkExtension;
import com.hbm.util.datafix.HbmDataFixers;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Radiation, multiblock core index, pending fallers and duct fluids travel with the chunk NBT.
 *
 * <p>backport: 26.x splits chunk IO into SerializableChunkData (parse/copyOf on the main thread,
 * write/read later) and keeps the values on that object in between. 1.21.1 has no
 * SerializableChunkData: ChunkSerializer.read(level, poi, info, pos, tag) builds the ProtoChunk
 * straight from the tag and ChunkSerializer.write(level, chunk) produces the tag on the main
 * thread, so both halves are done directly there (class name kept for the mixin config).
 */
@Mixin(ChunkSerializer.class)
public abstract class MixinSerializableChunkData {

    @Inject(method = "read", at = @At("RETURN"))
    private static void hbm$readNtmData(
            ServerLevel level,
            PoiManager poiManager,
            RegionStorageInfo regionInfo,
            ChunkPos pos,
            CompoundTag chunkData,
            CallbackInfoReturnable<ProtoChunk> cir) {
        ProtoChunk result = cir.getReturnValue();
        if (result == null) return;
        ChunkAccess target =
                (result instanceof ImposterProtoChunk imposter) ? imposter.getWrapped() : result;
        byte[] rad = Nbt.getByteArray(chunkData, IChunkExtension.RADIATION_NBT_KEY).orElse(null);
        if (rad != null) target.hbm$setRadiation(rad);
        long[] entries =
                Nbt.getLongArray(chunkData, IChunkExtension.CORE_INDEX_NBT_KEY).orElse(null);
        if (entries != null) {
            target.hbm$setCoreIndex(entries, entries.length);
            MultiblockCoreIndex.resolveLegacy(target, level);
        }
        PendingFallers.load(
                Nbt.getListOrEmpty(chunkData, IChunkExtension.PENDING_FALLERS_NBT_KEY), target);
        CompoundTag ducts =
                Nbt.getCompound(chunkData, IChunkExtension.DUCT_FLUIDS_NBT_KEY).orElse(null);
        if (ducts != null) {
            long[] positions = Nbt.getLongArray(ducts, "pos").orElseThrow();
            ListTag ids = Nbt.getListOrEmpty(ducts, "fluid");
            if (ids.size() != positions.length)
                throw new IllegalStateException("duct fluids: " + ducts);
            Long2ObjectOpenHashMap<String> fluids = new Long2ObjectOpenHashMap<>(positions.length);
            for (int i = 0; i < positions.length; i++)
                fluids.put(positions[i], Nbt.getStringOr(ids, i, ""));
            target.hbm$setDuctFluids(fluids);
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private static void hbm$writeNtmData(
            ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        if (tag == null) return;
        HbmDataFixers.stamp(tag);
        long[] entries = MultiblockCoreIndex.copyForSave(chunk);
        if (entries != null && entries.length != 0)
            tag.putLongArray(
                    IChunkExtension.CORE_INDEX_NBT_KEY, Arrays.copyOf(entries, entries.length));
        int fallers = chunk.hbm$pendingFallerSize();
        if (fallers != 0) {
            tag.put(
                    IChunkExtension.PENDING_FALLERS_NBT_KEY,
                    PendingFallers.save(
                            chunk.hbm$pendingFallerPositions(),
                            chunk.hbm$pendingFallerStates(),
                            fallers));
        }
        // 26.x copyOf only carries a non-empty map over to write
        Long2ObjectOpenHashMap<String> fluids = chunk.hbm$ductFluids();
        if (fluids != null && !fluids.isEmpty()) {
            long[] positions = new long[fluids.size()];
            ListTag ids = new ListTag();
            int i = 0;
            for (var entry : fluids.long2ObjectEntrySet()) {
                positions[i++] = entry.getLongKey();
                ids.add(StringTag.valueOf(entry.getValue()));
            }
            CompoundTag out = new CompoundTag();
            out.putLongArray("pos", positions);
            out.put("fluid", ids);
            tag.put(IChunkExtension.DUCT_FLUIDS_NBT_KEY, out);
        }
    }
}
