// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.BlockEntityCompat;
import com.hbm.backport.SubLevelSpace;
import net.minecraft.world.phys.Vec3;

public class BlockEntityChlorineSeal extends BlockEntityCompat {

    private static final int MAX_STEPS = 50;

    public BlockEntityChlorineSeal(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHLORINE_SEAL.get(), pos, state);
    }

    public static void tick(
            Level level, BlockPos pos, BlockState state, BlockEntityChlorineSeal be) {
        if (SubLevelSpace.inSubLevel(level, pos.getX() + 0.5D, pos.getZ() + 0.5D)) {
            spreadFromSubLevel(level, pos);
            return;
        }
        spread(level, pos);
    }

    /**
     * backport-fix: BF-044 — a seal on a physics build. The gas takes the same random walk through
     * the build, so its walls and hull still hold the gas back, but every gas block lands in the
     * world where that spot of the build is drawn: that's where players and mobs actually are.
     * The cloud stays where it was let out while the build moves on.
     */
    private static void spreadFromSubLevel(Level level, BlockPos start) {
        Block gas = ModBlocks.CHLORINE_GAS.get();
        Block seal = ModBlocks.VENT_CHLORINE_SEAL.get();
        RandomSource rand = level.getRandom();
        BlockPos.MutableBlockPos cursor = start.mutable();

        // the build's pose around the seal, once per tick: world = o + dx*ax + dy*ay + dz*az
        double x = start.getX() + 0.5D, y = start.getY() + 0.5D, z = start.getZ() + 0.5D;
        Vec3 o = SubLevelSpace.toWorld(level, x, y, z);
        Vec3 ax = SubLevelSpace.toWorld(level, x + 1D, y, z).subtract(o);
        Vec3 ay = SubLevelSpace.toWorld(level, x, y + 1D, z).subtract(o);
        Vec3 az = SubLevelSpace.toWorld(level, x, y, z + 1D).subtract(o);

        for (int step = 0; step <= MAX_STEPS; step++) {
            BlockState at = level.getBlockState(cursor);
            if (!at.is(seal)) {
                if (!at.canBeReplaced() && !at.is(gas)) return;

                int dx = cursor.getX() - start.getX();
                int dy = cursor.getY() - start.getY();
                int dz = cursor.getZ() - start.getZ();
                BlockPos out =
                        BlockPos.containing(
                                o.add(ax.scale(dx)).add(ay.scale(dy)).add(az.scale(dz)));
                if (!level.isLoaded(out)) return;
                BlockState there = level.getBlockState(out);
                if (there.canBeReplaced() && !there.is(gas)) {
                    level.setBlockAndUpdate(out, gas.defaultBlockState());
                } else if (!there.is(gas)) {
                    return;
                }
            }
            cursor.move(Direction.from3DDataValue(rand.nextInt(6)));
        }
    }

    private static void spread(Level level, BlockPos start) {
        Block gas = ModBlocks.CHLORINE_GAS.get();
        Block seal = ModBlocks.VENT_CHLORINE_SEAL.get();
        RandomSource rand = level.getRandom();
        BlockPos.MutableBlockPos cursor = start.mutable();

        for (int step = 0; step <= MAX_STEPS; step++) {
            BlockState at = level.getBlockState(cursor);
            if (at.canBeReplaced()) {
                level.setBlockAndUpdate(cursor, gas.defaultBlockState());
                at = level.getBlockState(cursor);
            }

            if (!at.is(gas) && !at.is(seal)) return;
            cursor.move(Direction.from3DDataValue(rand.nextInt(6)));
        }
    }
}
