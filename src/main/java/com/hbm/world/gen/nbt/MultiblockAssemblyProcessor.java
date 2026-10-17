// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import com.hbm.backport.StructureProcessorCompat;
import org.jspecify.annotations.Nullable;

public class MultiblockAssemblyProcessor extends StructureProcessorCompat {

    public static final MapCodec<MultiblockAssemblyProcessor> MAP_CODEC =
            MapCodec.unit(() -> MultiblockAssemblyProcessor.INSTANCE);
    public static final MultiblockAssemblyProcessor INSTANCE = new MultiblockAssemblyProcessor();

    private MultiblockAssemblyProcessor() {}

    // backport: 26.x evaluatesEntirePieceState() = true has no 1.21.1 hook; 1.21.1's
    // StructureTemplate.placeInWorld already finalizes the whole template on every chunk it is
    // placed into, and the clip (the box being placed) keeps the writes inside that chunk, which
    // also covers the earlier backport fix for setBlock into far chunks (e.g. hbm:oil_rig)

    private static void assembleFolded(
            ServerLevelAccessor level,
            BlockPos pos,
            BlockState coreState,
            BlockMultiblockCore block,
            @Nullable BoundingBox clip) {
        if (clip == null || clip.isInside(pos)) level.setBlock(pos, coreState, 3);
        block.fillSpace(level, pos, coreState.getValue(BlockMultiblockCore.FACING), clip);
        block.assembled(level, pos, coreState, clip);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos position,
            BlockPos referencePos,
            List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
            List<StructureTemplate.StructureBlockInfo> processedBlockInfoList,
            StructurePlaceSettings settings) {
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfoList) {
            if (info.state().getBlock() instanceof BlockMultiblockCore folded) {
                assembleFolded(
                        level,
                        info.pos(),
                        info.state().mirror(settings.getMirror()).rotate(settings.getRotation()),
                        folded,
                        settings.getBoundingBox());
            }
        }
        return processedBlockInfoList;
    }

    @Override
    public MapCodec<MultiblockAssemblyProcessor> codec() {
        return MAP_CODEC;
    }
}
