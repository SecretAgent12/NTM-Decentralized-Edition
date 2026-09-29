// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import com.hbm.backport.ProblemReporter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.storage.TagValueInput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hbm.backport.BlockEntityCompat;

public final class CarriedBlockItems {
    private static final Logger LOGGER = LoggerFactory.getLogger("NTM");

    private CarriedBlockItems() {}

    public static @Nullable BlockEntity load(
            BlockState state, BlockPos pos, CompoundTag data, HolderLookup.Provider registries) {
        String owner = state.getBlock().getClass().getName();

        if (!owner.startsWith("net.minecraft.") && !owner.startsWith("com.hbm.")) return null;
        BlockEntity blockEntity = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);

        try (ProblemReporter.ScopedCollector reporter =
                new ProblemReporter.ScopedCollector(blockEntity.toString(), LOGGER)) {
            BlockEntityCompat.loadWithComponents(blockEntity, TagValueInput.tagOf(TagValueInput.create(reporter, registries, data)));
        }
        return blockEntity;
    }
}
