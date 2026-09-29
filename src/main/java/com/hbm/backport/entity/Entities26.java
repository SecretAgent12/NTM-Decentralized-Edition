// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 26.x entity-side helpers that 1.21.1 lacks (call sites are rewritten to use them).
 */
public final class Entities26 {

    /**
     * 26.x Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS: the replaced block entity's
     * preRemoveSideEffects (container drops etc.) are skipped. 1.21.1 has no such flag; the
     * bit is free there (1.21.1 flags end at UPDATE_MOVE_BY_PISTON = 64) and is honoured by
     * {@code com.hbm.mixin.backport.EntitySkipBlockEntitySideEffectsMixin} on Level.setBlock.
     */
    public static final int UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS = 256;

    private Entities26() {}

    /** 26.x Goal.getServerLevel(Entity). */
    public static ServerLevel getServerLevel(Entity entity) {
        return (ServerLevel) entity.level();
    }

    /** 26.x VoxelShape.move(Vec3i). */
    public static VoxelShape move(VoxelShape shape, Vec3i offset) {
        return shape.move(offset.getX(), offset.getY(), offset.getZ());
    }

    /**
     * 26.x PathNavigation.setCanOpenDoors(boolean); on 1.21.1 only GroundPathNavigation has it,
     * and it just forwards to the node evaluator, which every navigation has.
     */
    public static void setCanOpenDoors(PathNavigation navigation, boolean canOpenDoors) {
        navigation.getNodeEvaluator().setCanOpenDoors(canOpenDoors);
    }

    /**
     * 26.x block tag #minecraft:grass_blocks (1.21.1 has no such tag).
     * backport: unverified: contents assumed to be grass_block, mycelium, podzol.
     */
    public static boolean isGrassBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL);
    }

    /**
     * Level.setBlock with 26.x UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS semantics: when the block
     * changes, its block entity is removed first, so 1.21.1's onRemove (and the backport's
     * preRemoveSideEffects bridge) find none and drop nothing.
     */
    public static void removeBlockEntityQuietly(Level level, BlockPos pos, BlockState newState) {
        BlockState old = level.getBlockState(pos);
        if (old.hasBlockEntity() && !old.is(newState.getBlock())) level.removeBlockEntity(pos);
    }

    /** Strips the 26.x-only flag bits before 1.21.1 sees them. */
    public static int vanillaFlags(int flags) {
        return flags & ~UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    }
}
