// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.hbm.backport.compat.BushBlockCompat;

public class BlockDeadPlant extends BushBlockCompat {

    public static final MapCodec<BlockDeadPlant> CODEC = simpleCodec(BlockDeadPlant::new);
    private static final VoxelShape SHAPE = com.hbm.backport.Shapes26.column(12.0, 0.0, 13.0);

    public BlockDeadPlant(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockDeadPlant> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(ModBlocks.WASTE_EARTH.get())
                || state.is(ModBlocks.DIRT_OILY.get())
                || state.is(ModBlocks.DIRT_DEAD.get());
    }
}
