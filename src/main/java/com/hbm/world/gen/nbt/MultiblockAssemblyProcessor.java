// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import com.hbm.backport.StructureProcessorCompat;

public class MultiblockAssemblyProcessor extends StructureProcessorCompat {

    public static final MapCodec<MultiblockAssemblyProcessor> MAP_CODEC =
            MapCodec.unit(() -> MultiblockAssemblyProcessor.INSTANCE);
    public static final MultiblockAssemblyProcessor INSTANCE = new MultiblockAssemblyProcessor();

    private MultiblockAssemblyProcessor() {}

    private static void assembleFolded(
            ServerLevelAccessor level,
            BlockPos pos,
            BlockState coreState,
            BlockMultiblockCore block) {
        level.setBlock(pos, coreState, 3);
        block.fillSpace(level, pos, coreState.getValue(BlockMultiblockCore.FACING));
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos position,
            BlockPos referencePos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfoList,
            StructurePlaceSettings settings) {

        List<BlockPos> cores = new ArrayList<>();
        // backport: 1.21.1 StructureTemplate.placeInWorld finalizes the whole template on every
        // chunk it is placed into and clips only the final block writes to that chunk's box, so a
        // core in another chunk was assembled from here as well - setBlock into a far chunk
        // ("Detected setBlock in a far chunk" during worldgen, e.g. hbm:oil_rig) and the same
        // multiblock assembled once per chunk of the structure. Assemble only the cores inside the
        // box being placed; their cells reach at most into the neighbouring chunks, which the
        // worldgen region allows.
        net.minecraft.world.level.levelgen.structure.BoundingBox box = settings.getBoundingBox();
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfoList) {
            BlockState placedState =
                    info.state().mirror(settings.getMirror()).rotate(settings.getRotation());

            if (placedState.getBlock() instanceof BlockMultiblockCore folded) {
                if (box == null || box.isInside(info.pos()))
                    assembleFolded(level, info.pos(), placedState, folded);
                cores.add(info.pos());
            }
        }
        if (cores.isEmpty()) return processedBlockInfoList;

        List<StructureTemplate.StructureBlockInfo> kept =
                new ArrayList<>(processedBlockInfoList.size());
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfoList) {
            if (!cores.contains(info.pos()) && claimed(level, cores, info.pos())) continue;
            kept.add(info);
        }
        return kept;
    }

    private static boolean claimed(ServerLevelAccessor level, List<BlockPos> cores, BlockPos pos) {
        for (BlockPos core : cores) {
            if (MultiblockSurface.coreClaims(level, core, pos)) return true;
        }
        return false;
    }

    @Override
    public MapCodec<MultiblockAssemblyProcessor> codec() {
        return MAP_CODEC;
    }
}
