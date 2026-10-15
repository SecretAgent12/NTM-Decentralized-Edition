// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

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

    /**
     * 26.x LivingEntity.dropFromLootTable(level, source, playerKilled, table): 1.21.1's death-loot roll
     * (LivingEntity.dropFromLootTable(source, playerKilled)) with an explicit table. {@code lastHurtByPlayer} is
     * the entity's protected field of the same name (passed by the subclass calling this).
     */
    public static void dropFromLootTable(
            LivingEntity entity,
            ServerLevel level,
            DamageSource source,
            boolean playerKilled,
            @Nullable Player lastHurtByPlayer,
            ResourceKey<LootTable> table) {
        LootTable loot = level.getServer().reloadableRegistries().getLootTable(table);
        LootParams.Builder builder =
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.THIS_ENTITY, entity)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                        .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                        .withOptionalParameter(
                                LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
        if (playerKilled && lastHurtByPlayer != null)
            builder =
                    builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, lastHurtByPlayer)
                            .withLuck(lastHurtByPlayer.getLuck());
        loot.getRandomItems(
                builder.create(LootContextParamSets.ENTITY), entity.getLootTableSeed(), entity::spawnAtLocation);
    }

    /** 26.x LivingEntity.dropFromGiftLootTable(level, table, output). */
    public static boolean dropFromGiftLootTable(
            Entity entity, ServerLevel level, ResourceKey<LootTable> table, BiConsumer<ServerLevel, ItemStack> output) {
        LootParams params =
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withParameter(LootContextParams.THIS_ENTITY, entity)
                        .create(LootContextParamSets.GIFT);
        return dropAll(level, table, params, output);
    }

    /**
     * 26.x Entity.dropFromEntityInteractLootTable(level, table, interacting, tool, output). 1.21.1 has neither the
     * entity_interact parameter set nor INTERACTING_ENTITY, so the parameters (this entity, origin, tool) are passed
     * unvalidated; the interacting entity is not available to the table.
     */
    public static boolean dropFromEntityInteractLootTable(
            Entity entity,
            ServerLevel level,
            ResourceKey<LootTable> table,
            @Nullable Entity interacting,
            ItemStack tool,
            BiConsumer<ServerLevel, ItemStack> output) {
        Map<LootContextParam<?>, Object> values = new HashMap<>();
        values.put(LootContextParams.THIS_ENTITY, entity);
        values.put(LootContextParams.ORIGIN, entity.position());
        values.put(LootContextParams.TOOL, tool);
        return dropAll(level, table, new LootParams(level, values, Map.of(), 0F), output);
    }

    private static boolean dropAll(
            ServerLevel level, ResourceKey<LootTable> table, LootParams params, BiConsumer<ServerLevel, ItemStack> output) {
        var items = level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params);
        if (items.isEmpty()) return false;
        items.forEach(stack -> output.accept(level, stack));
        return true;
    }
}
