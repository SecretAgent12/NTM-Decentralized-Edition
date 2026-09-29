// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.hbm.backport.client.blockmodel.CardinalLighting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.block.MovingBlockRenderState}: one block drawn away from
 * its level position (falling blocks), presented to the block renderer as a one-block level.
 */
public class MovingBlockRenderState implements BlockAndTintGetter {
    public BlockPos randomSeedPos = BlockPos.ZERO;
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public @Nullable Holder<Biome> biome;
    public @Nullable LevelLightEngine lightEngine;
    public CardinalLighting cardinalLighting = CardinalLighting.DEFAULT;

    @Override
    public float getShade(Direction direction, boolean shade) {
        return cardinalLighting.shade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        if (lightEngine != null) return lightEngine;
        throw new IllegalStateException("moving block without a light engine");
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        if (biome == null) return -1;
        return resolver.getColor(biome.value(), pos.getX(), pos.getZ());
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return pos.equals(blockPos) ? blockState : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return 1;
    }

    @Override
    public int getMinBuildHeight() {
        return blockPos.getY();
    }
}
