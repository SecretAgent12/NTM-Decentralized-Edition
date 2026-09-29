// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

/** 26.x chunk-section helpers that 1.21.1 lacks. */
public final class Sections {

    private Sections() {}

    /** 26.x PalettedContainerFactory.createForBlockStates(): an all-air block container. */
    public static PalettedContainer<BlockState> emptyStates() {
        return new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(),
                PalettedContainer.Strategy.SECTION_STATES);
    }

    /*
     * backport: 1.21.1 PalettedContainer.copy() is only safe for READ-ONLY snapshots: Data.copy()
     * -> Palette.copy() keeps the SOURCE container as the palette's resize handler (26.x
     * Palette.copy(PaletteResize) rebinds it to the new container). Writing a state that is not
     * yet in a full palette of such a copy then grows the copy's palette past what its bit
     * storage can index while resizing the source container instead, so a later write stores
     * index 16 into 4-bit storage ("The value 16 is not in the specified inclusive range of 0 to
     * 15" in SimpleBitStorage.getAndSet). ntm-next writes into its section copies (ChunkUtil
     * carve, SectionSnapshot) and then installs them in the live chunk, so copies here must
     * rebuild the palette with the new container as its resize handler.
     */

    /** 26.x PalettedContainerRO.copy(): a writable deep copy (see above). */
    public static PalettedContainer<Holder<Biome>> copyBiomes(PalettedContainerRO<Holder<Biome>> biomes) {
        PalettedContainer<Holder<Biome>> src = (PalettedContainer<Holder<Biome>>) biomes;
        // recreate() keeps registry + strategy and is bound to itself; 64 cells, copied by value
        PalettedContainer<Holder<Biome>> dst = src.recreate();
        for (int y = 0; y < 4; y++)
            for (int z = 0; z < 4; z++)
                for (int x = 0; x < 4; x++) dst.getAndSetUnchecked(x, y, z, src.get(x, y, z));
        return dst;
    }

    /** 26.x PalettedContainer.copy() for block states: a writable deep copy (see above). */
    public static PalettedContainer<BlockState> copyStates(PalettedContainer<BlockState> src) {
        PalettedContainer.Data<BlockState> data = src.data;
        net.minecraft.util.BitStorage storage = data.storage().copy();
        net.minecraft.world.level.chunk.Palette<BlockState> palette = data.palette();
        int bits = storage.getBits();
        java.util.List<BlockState> entries;
        if (bits > 8) {
            entries = java.util.List.of(); // global palette: storage holds registry ids
        } else {
            // a palette can hold one entry past its capacity (added right before a resize of
            // its container); the storage never references it, so it is dropped here
            int size = palette.getSize();
            if (bits > 0) size = Math.min(size, 1 << bits);
            else size = Math.min(size, 1);
            BlockState[] values = new BlockState[size];
            for (int i = 0; i < size; i++) values[i] = palette.valueFor(i);
            entries = java.util.Arrays.asList(values);
        }
        // public ctor: builds the palette from the entries with the new container as its handler
        return new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, PalettedContainer.Strategy.SECTION_STATES,
                data.configuration(), storage, entries);
    }

    /** 26.x LevelChunkSection.copy(). */
    public static LevelChunkSection copy(LevelChunkSection section) {
        return new LevelChunkSection(copyStates(section.getStates()), copyBiomes(section.getBiomes()));
    }

    /** 26.x ChunkPos.isValid(x, z), taken as: inside the +-30M block world limit (unverified against 26.x). */
    public static boolean validChunk(int x, int z) {
        int max = net.minecraft.core.SectionPos.blockToSectionCoord(30_000_000);
        return x >= -max && x <= max && z >= -max && z <= max;
    }
}
