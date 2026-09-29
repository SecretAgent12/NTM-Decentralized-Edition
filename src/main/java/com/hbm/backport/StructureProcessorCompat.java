// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.mojang.serialization.MapCodec;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.Nullable;

/**
 * 26.x StructureProcessor (an interface registered by its MapCodec, with a
 * template-relative position passed to processBlock) on 1.21.1's abstract class
 * (registered as a StructureProcessorType, raw block info passed instead).
 *
 * The registrar records which StructureProcessorType it created for each codec
 * (register()), and getType() looks it up.
 */
public abstract class StructureProcessorCompat extends StructureProcessor {

    private static final Map<MapCodec<?>, StructureProcessorType<?>> TYPES = new IdentityHashMap<>();

    public static synchronized <T extends StructureProcessor> StructureProcessorType<T> register(MapCodec<T> codec) {
        StructureProcessorType<T> type = () -> codec;
        TYPES.put(codec, type);
        return type;
    }

    public abstract MapCodec<? extends StructureProcessor> codec();

    /** The type the registrar created for a codec (also for processors that extend a vanilla one). */
    public static synchronized StructureProcessorType<?> typeOf(MapCodec<?> codec) {
        StructureProcessorType<?> type = TYPES.get(codec);
        if (type == null) throw new IllegalStateException("no registered processor type for " + codec);
        return type;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return typeOf(codec());
    }

    @Override
    public final @Nullable StructureBlockInfo processBlock(
            LevelReader level, BlockPos offset, BlockPos pos, StructureBlockInfo raw, StructureBlockInfo processed,
            StructurePlaceSettings settings) {
        return processBlock(level, offset, pos, raw.pos(), processed, settings);
    }

    /** 26.x form: templateRelativePos is the block's position inside the template. */
    public @Nullable StructureBlockInfo processBlock(
            LevelReader level, BlockPos targetPosition, BlockPos referencePos, BlockPos templateRelativePos,
            StructureBlockInfo processedBlockInfo, StructurePlaceSettings settings) {
        return processedBlockInfo;
    }
}
