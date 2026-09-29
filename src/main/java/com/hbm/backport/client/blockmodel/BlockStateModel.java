// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 26.x net.minecraft.client.renderer.block.dispatch.BlockStateModel (with the NeoForge 26
 * level-aware extensions). Drawn in 1.21.1 through {@link BlockModelAdapter}.
 */
public interface BlockStateModel {

    void collectParts(RandomSource random, List<BlockStateModelPart> output);

    /** NeoForge 26: level-aware part selection (connected textures, neighbour masks...). */
    default void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        collectParts(random, output);
    }

    default List<BlockStateModelPart> collectParts(RandomSource random) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        collectParts(random, parts);
        return parts;
    }

    Material.Baked particleMaterial();

    /** NeoForge 26: level-aware particle material. */
    default Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return particleMaterial();
    }

    int materialFlags();

    default boolean hasMaterialFlag(int flag) {
        return (materialFlags() & flag) != 0;
    }

    /**
     * NeoForge 26: a key equal for positions producing the same geometry (null: the geometry does
     * not depend on the level).
     */
    default @Nullable Object createGeometryKey(
            BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return null;
    }

    /** 26.x BlockStateModel.Unbaked. */
    interface Unbaked extends ResolvableModel {
        BlockStateModel bake(ModelBaker baker);

        default UnbakedRoot asRoot() {
            Unbaked self = this;
            return new UnbakedRoot() {
                @Override
                public BlockStateModel bake(BlockState state, ModelBaker baker) {
                    return baker.compute(new CachedKey(self));
                }

                @Override
                public Object visualEqualityGroup(BlockState state) {
                    return this;
                }

                @Override
                public void resolveDependencies(Resolver resolver) {
                    self.resolveDependencies(resolver);
                }
            };
        }
    }

    record CachedKey(Unbaked unbaked) implements ModelBaker.SharedOperationKey<BlockStateModel> {
        @Override
        public BlockStateModel compute(ModelBaker baker) {
            return unbaked.bake(baker);
        }
    }

    /** 26.x BlockStateModel.UnbakedRoot: what a blockstate definition yields per state. */
    interface UnbakedRoot extends ResolvableModel {
        BlockStateModel bake(BlockState state, ModelBaker baker);

        Object visualEqualityGroup(BlockState state);
    }
}
