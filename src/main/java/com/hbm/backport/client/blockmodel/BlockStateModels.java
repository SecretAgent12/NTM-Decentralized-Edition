// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.hbm.backport.client.rendertype.ChunkSectionLayer;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 1.21.1 baked block models seen as 26.x {@link BlockStateModel}s: HBM's own adapters unwrap to
 * their model; any other model is converted once (quads per render type, decoded with that
 * layer). Deviation: a 1.21.1 model that varies with the random source (weighted variants) is
 * sampled once with a fixed seed.
 */
public final class BlockStateModels {
    private static final Map<BlockState, BlockStateModel> WRAPPED = new IdentityHashMap<>();

    private BlockStateModels() {}

    /** Forget converted models (new model reload). */
    public static synchronized void invalidate() {
        WRAPPED.clear();
    }

    public static BlockStateModel get(BlockState state) {
        return of(Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state);
    }

    public static BlockStateModel of(BakedModel model, BlockState state) {
        if (model instanceof BlockModelAdapter adapter) return adapter.model();
        synchronized (BlockStateModels.class) {
            BlockStateModel wrapped = WRAPPED.get(state);
            if (wrapped instanceof Wrapped w && w.source == model) return wrapped;
            wrapped = wrap(model, state);
            WRAPPED.put(state, wrapped);
            return wrapped;
        }
    }

    public static BlockStateModel wrap(BakedModel model, BlockState state) {
        return new Wrapped(model, new SingleVariant(convert(model, state)));
    }

    public static SimpleModelWrapper convert(BakedModel model, BlockState state) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        RandomSource random = RandomSource.create(42L);
        for (RenderType type : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            ChunkSectionLayer layer = Layers.layer(type);
            random.setSeed(42L);
            for (var quad : model.getQuads(state, null, random, ModelData.EMPTY, type))
                builder.addUnculledFace(BakedQuad.fromVanilla(quad, layer));
            for (Direction face : Direction.values()) {
                random.setSeed(42L);
                for (var quad : model.getQuads(state, face, random, ModelData.EMPTY, type))
                    builder.addCulledFace(face, BakedQuad.fromVanilla(quad, layer));
            }
        }
        return new SimpleModelWrapper(
                builder.build(), model.useAmbientOcclusion(), new Material.Baked(model.getParticleIcon(ModelData.EMPTY)));
    }

    private record Wrapped(BakedModel source, SingleVariant model) implements BlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            model.collectParts(random, output);
        }

        @Override
        public Material.Baked particleMaterial() {
            return model.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return model.materialFlags();
        }
    }
}
