// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import com.hbm.backport.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import com.hbm.backport.StructureProcessorCompat;

public final class SpawnerEntityProcessor extends StructureProcessorCompat {
    // backport: a record in 26.x; a record cannot extend the 1.21.1 base class
    private final WeightedList<EntityType<?>> mobs;

    public SpawnerEntityProcessor(WeightedList<EntityType<?>> mobs) {
        this.mobs = mobs;
    }

    public WeightedList<EntityType<?>> mobs() {
        return mobs;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SpawnerEntityProcessor o && java.util.Objects.equals(mobs, o.mobs);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(mobs);
    }

    @Override
    public String toString() {
        return "SpawnerEntityProcessor[" + "mobs=" + mobs + "]";
    }


    public static final MapCodec<SpawnerEntityProcessor> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            WeightedList.nonEmptyCodec(
                                                            BuiltInRegistries.ENTITY_TYPE
                                                                    .byNameCodec())
                                                    .fieldOf("mobs")
                                                    .forGetter(SpawnerEntityProcessor::mobs))
                                    .apply(i, SpawnerEntityProcessor::new));

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        if (!processedBlockInfo.state().is(Blocks.SPAWNER)) return processedBlockInfo;

        BlockPos pos = processedBlockInfo.pos();
        EntityType<?> mob =
                ProcessorRandom.weighted(
                        level,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        ProcessorRandom.SPAWNER_ENTITY,
                        mobs);
        CompoundTag entity = new CompoundTag();
        entity.putString("id", EntityType.getKey(mob).toString());
        CompoundTag spawnData = new CompoundTag();
        spawnData.put(SpawnData.ENTITY_TAG, entity);
        CompoundTag nbt =
                processedBlockInfo.nbt() == null
                        ? new CompoundTag()
                        : processedBlockInfo.nbt().copy();
        // backport: 1.21.1 has no BlockEntityTypeIds; the type's registry name
        nbt.putString(
                "id",
                BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityType.MOB_SPAWNER).toString());
        nbt.put(BaseSpawner.SPAWN_DATA_TAG, spawnData);
        return new StructureTemplate.StructureBlockInfo(pos, processedBlockInfo.state(), nbt);
    }

    @Override
    public MapCodec<SpawnerEntityProcessor> codec() {
        return MAP_CODEC;
    }
}
