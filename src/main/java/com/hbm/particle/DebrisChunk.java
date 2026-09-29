// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public final class DebrisChunk implements BlockAndTintGetter {

    public final int sizeX, sizeY, sizeZ;
    private final BlockState[] blocks;
    private final BlockState air = Blocks.AIR.defaultBlockState();
    private final int blockLight;
    private final int skyLight;
    // backport: 26.x keeps the level's CardinalLighting; 1.21.1 asks getShade(direction, shade)
    private final float[] shade = new float[12];
    private final int tint;

    public DebrisChunk(int sizeX, int sizeY, int sizeZ, ClientLevel level, BlockPos origin) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = new BlockState[Math.max(0, sizeX * sizeY * sizeZ)];
        this.blockLight = level.getBrightness(LightLayer.BLOCK, origin);
        this.skyLight = level.getBrightness(LightLayer.SKY, origin);
        for (Direction dir : Direction.values()) {
            shade[dir.ordinal()] = level.getShade(dir, true);
            shade[6 + dir.ordinal()] = level.getShade(dir, false);
        }
        this.tint = -1;
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    public BlockState get(int x, int y, int z) {
        if (!inBounds(x, y, z)) return air;
        BlockState state = blocks[(y * sizeZ + z) * sizeX + x];
        return state == null ? air : state;
    }

    public void set(int x, int y, int z, BlockState state) {
        if (!inBounds(x, y, z)) return;
        blocks[(y * sizeZ + z) * sizeX + x] = state;
    }

    public boolean isEmpty() {
        for (BlockState state : blocks) {
            if (state != null && !state.isAir()) return false;
        }
        return true;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return get(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return NO_LIGHT;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return layer == LightLayer.SKY ? skyLight : blockLight;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return Math.max(blockLight, skyLight - darkening);
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return this.shade[(shade ? 0 : 6) + direction.ordinal()];
    }

    // backport: 1.21.1 has no LevelLightEngine.EMPTY; an engine without sky/block storage (light
    // queries go through the getBrightness overrides above)
    private static final LevelLightEngine NO_LIGHT =
            new LevelLightEngine(
                    new LightChunkGetter() {
                        @Override
                        public @Nullable LightChunk getChunkForLighting(int x, int z) {
                            return null;
                        }

                        @Override
                        public BlockGetter getLevel() {
                            return net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
                        }
                    },
                    false,
                    false);

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver color) {
        return tint;
    }

    @Override
    public int getHeight() {
        return sizeY;
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }
}
