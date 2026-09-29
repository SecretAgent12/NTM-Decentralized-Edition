// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.hbm.client.model.HbmModelJson;
import com.hbm.client.model.OccluderMasks;
import com.hbm.client.model.SectionedModel;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registration of the 26.x block model system on 1.21.1 (call {@link #register} on the mod bus,
 * client side). Replaces NeoForge 26's RegisterBlockStateModels ("hbm:baked" custom blockstate
 * definition) and ModelEvent.RegisterLoaders ("hbm:obj"), plus the 26.x ModelManager mixin that
 * derived occluder masks and wrapped sectioned models:
 *
 * <ol>
 *   <li>RegisterSpriteSourceTypesEvent: "hbm:padded" atlas source (PaddedSpriteSource).
 *   <li>RegisterGeometryLoaders: "hbm:obj" -> {@link GeometryAdapter} over HbmModelJson.obj.
 *   <li>RegisterAdditional: builds the unbaked roots of every block registered with
 *       Reg...bakedBy (all its states, as the hbm:baked definition did) and registers every model
 *       they depend on as a standalone model, so the bakery loads and resolves them.
 *   <li>ModifyBakingResult: re-derives the model groups of those states from the roots'
 *       visualEqualityGroup (1.21.1 re-renders a section on a state change only across groups);
 *       bakes each root with a {@link Baker} over the bakery and the stitched
 *       atlas; derives OccluderMasks and SectionedModel wrapping over the HBM blocks' models (baked
 *       roots + converted JSON models), and installs {@link BlockModelAdapter}s at the states'
 *       model locations.
 *   <li>BakingCompleted (main thread): SectionedModel.publish().
 * </ol>
 */
public final class BlockModels {
    private static final Logger LOGGER = LoggerFactory.getLogger("hbm/blockmodel");

    private static volatile Map<Block, Map<BlockState, BlockStateModel.UnbakedRoot>> pendingRoots = Map.of();

    private BlockModels() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(BlockModels::registerSpriteSources);
        modBus.addListener(BlockModels::registerLoaders);
        modBus.addListener(BlockModels::registerAdditional);
        modBus.addListener(BlockModels::modifyBakingResult);
        modBus.addListener(BlockModels::bakingCompleted);
    }

    private static void registerSpriteSources(net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent event) {
        com.hbm.client.model.PaddedSpriteSource.TYPE =
                event.register(Library.id("padded"), com.hbm.client.model.PaddedSpriteSource.MAP_CODEC);
    }

    private static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(Library.id("obj"), new GeometryAdapter.Loader(HbmModelJson::obj));
    }

    private static void registerAdditional(ModelEvent.RegisterAdditional event) {
        Map<Block, Map<BlockState, BlockStateModel.UnbakedRoot>> roots = new LinkedHashMap<>();
        Set<ResourceLocation> dependencies = new LinkedHashSet<>();
        ResolvableModel.Resolver resolver = dependencies::add;
        for (Map.Entry<ResourceLocation, Supplier<com.hbm.client.model.BlockModel<?>>> entry : Reg.bakedBy().entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getKey());
            try {
                com.hbm.client.model.BlockModel.Prepared prepared = entry.getValue().get().prepared();
                Map<BlockState, BlockStateModel.UnbakedRoot> byState = new IdentityHashMap<>();
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    BlockStateModel.UnbakedRoot root = prepared.root(block, state);
                    byState.put(state, root);
                    root.resolveDependencies(resolver);
                }
                roots.put(block, byState);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to build the block models of {}", entry.getKey(), e);
            }
        }
        for (ResourceLocation id : dependencies) event.register(ModelResourceLocation.standalone(id));
        pendingRoots = roots;
    }

    private static Function<ResourceLocation, UnbakedModel> bakeryModels(ModelBakery bakery) {
        Method getModel;
        try {
            getModel = ModelBakery.class.getDeclaredMethod("getModel", ResourceLocation.class);
            getModel.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ModelBakery#getModel is not accessible", e);
        }
        return id -> {
            try {
                return (UnbakedModel) getModel.invoke(bakery, id);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("ModelBakery#getModel(" + id + ") failed", e);
            }
        };
    }

    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BlockStateModels.invalidate();
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        Baker baker = new Baker(bakeryModels(event.getModelBakery()), event.getTextureGetter());

        Map<BlockState, BlockStateModel> baked = new IdentityHashMap<>();
        for (Map.Entry<Block, Map<BlockState, BlockStateModel.UnbakedRoot>> block : pendingRoots.entrySet()) {
            for (Map.Entry<BlockState, BlockStateModel.UnbakedRoot> entry : block.getValue().entrySet()) {
                try {
                    baked.put(entry.getKey(), entry.getValue().bake(entry.getKey(), baker));
                } catch (RuntimeException e) {
                    LOGGER.error("Failed to bake the block model of {}", entry.getKey(), e);
                }
            }
        }

        regroup(event.getModelBakery(), pendingRoots);

        // every HBM block's model (26.x: the BlockStateModelSet the mixin saw) for masks/sections
        Map<BlockState, BlockStateModel> all = new IdentityHashMap<>(baked);
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!com.hbm.NuclearTech.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (all.containsKey(state)) continue;
                BakedModel vanilla = models.get(BlockModelShaper.stateToModelLocation(state));
                if (vanilla == null) continue;
                try {
                    all.put(state, BlockStateModels.of(vanilla, state));
                } catch (RuntimeException e) {
                    LOGGER.warn("Could not convert the model of {}", state, e);
                }
            }
        }
        try {
            OccluderMasks.derive(all);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to derive occluder masks", e);
        }
        Map<BlockState, BlockStateModel> published;
        try {
            published = SectionedModel.wrap(all);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to build sectioned models", e);
            published = all;
        }

        Map<BlockStateModel, BlockModelAdapter> adapters = new IdentityHashMap<>();
        List<BlockState> installed = new ArrayList<>();
        for (Map.Entry<BlockState, BlockStateModel> entry : published.entrySet()) {
            BlockState state = entry.getKey();
            BlockStateModel model = entry.getValue();
            if (!baked.containsKey(state) && model == all.get(state)) continue; // untouched JSON model
            BlockModelAdapter adapter = adapters.computeIfAbsent(model, BlockModelAdapter::new);
            models.put(BlockModelShaper.stateToModelLocation(state), adapter);
            installed.add(state);
        }
        LOGGER.debug("Installed {} HBM block state models ({} distinct)", installed.size(), adapters.size());
    }

    /**
     * 26.x decides whether a state change re-renders the section by the roots'
     * visualEqualityGroup. 1.21.1 decides by ModelManager's model groups, which the bakery built
     * from the blockstate JSON (for these blocks a single placeholder variant, so every state would
     * share one group and never re-render). States of a baked block get one group per distinct
     * visualEqualityGroup; a state alone in its group gets none (-1: always re-render).
     */
    private static void regroup(ModelBakery bakery, Map<Block, Map<BlockState, BlockStateModel.UnbakedRoot>> roots) {
        it.unimi.dsi.fastutil.objects.Object2IntMap<BlockState> groups = bakery.getModelGroups();
        int next = 1;
        for (int id : groups.values()) next = Math.max(next, id + 1);
        for (Map<BlockState, BlockStateModel.UnbakedRoot> byState : roots.values()) {
            Map<Object, List<BlockState>> byGroup = new java.util.HashMap<>();
            for (Map.Entry<BlockState, BlockStateModel.UnbakedRoot> entry : byState.entrySet()) {
                BlockState state = entry.getKey();
                groups.removeInt(state);
                if (state.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) {
                    groups.put(state, 0);
                    continue;
                }
                Object key;
                try {
                    key = entry.getValue().visualEqualityGroup(state);
                } catch (RuntimeException e) {
                    continue;
                }
                if (key != null) byGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(state);
            }
            for (List<BlockState> states : byGroup.values()) {
                if (states.size() < 2) continue;
                int id = next++;
                for (BlockState state : states) groups.put(state, id);
            }
        }
    }

    private static void bakingCompleted(ModelEvent.BakingCompleted event) {
        BlockStateModels.invalidate();
        SectionedModel.publish();
    }
}
