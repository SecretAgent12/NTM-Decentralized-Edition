// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.StructureProcessorCompat;

public final class MaybePlaceProcessor extends StructureProcessorCompat {
    // backport: a record in 26.x; a record cannot extend the 1.21.1 base class
    private final BlockState marker;
    private final BlockState result;
    private final float probability;

    public MaybePlaceProcessor(BlockState marker, BlockState result, float probability) {
        this.marker = marker;
        this.result = result;
        this.probability = probability;
    }

    public BlockState marker() {
        return marker;
    }

    public BlockState result() {
        return result;
    }

    public float probability() {
        return probability;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof MaybePlaceProcessor o && java.util.Objects.equals(marker, o.marker) && java.util.Objects.equals(result, o.result) && java.util.Objects.equals(probability, o.probability);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(marker, result, probability);
    }

    @Override
    public String toString() {
        return "MaybePlaceProcessor[" + "marker=" + marker + ", " + "result=" + result + ", " + "probability=" + probability + "]";
    }


    public static final MapCodec<MaybePlaceProcessor> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            BlockState.CODEC
                                                    .fieldOf("marker")
                                                    .forGetter(MaybePlaceProcessor::marker),
                                            BlockState.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(MaybePlaceProcessor::result),
                                            Codec.floatRange(0.0F, 1.0F)
                                                    .fieldOf("probability")
                                                    .forGetter(MaybePlaceProcessor::probability))
                                    .apply(i, MaybePlaceProcessor::new));

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        if (!processedBlockInfo.state().equals(marker)) return processedBlockInfo;

        if (ProcessorRandom.unitFloat(level, processedBlockInfo.pos(), ProcessorRandom.MAYBE)
                > probability) {
            return null;
        }
        return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), result, null);
    }

    @Override
    public MapCodec<MaybePlaceProcessor> codec() {
        return MAP_CODEC;
    }
}
