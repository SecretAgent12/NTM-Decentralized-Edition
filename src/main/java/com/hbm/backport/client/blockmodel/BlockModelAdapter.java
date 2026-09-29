// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.hbm.backport.client.rendertype.ChunkSectionLayer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.jspecify.annotations.Nullable;

/**
 * A 26.x {@link BlockStateModel} as a 1.21.1 BakedModel.
 *
 * <ul>
 *   <li>Per-quad chunk layers: getRenderTypes is the union of the parts' quad layers and
 *       getQuads(..., renderType) returns the quads of that layer (all quads for a null or
 *       non-chunk type).
 *   <li>Level-aware models (NeoForge 26 collectParts(level, pos, ...)): the parts are collected in
 *       getModelData, which the 1.21.1 section compiler calls per block with the section region,
 *       and carried to getQuads in the model data.
 *   <li>Item use (hbm:obj item models): one render pass per item render type of the quads.
 * </ul>
 */
public class BlockModelAdapter implements IDynamicBakedModel {

    public static final ModelProperty<List<BlockStateModelPart>> PARTS = new ModelProperty<>();
    private static final Direction[] FACES = Direction.values();
    private static final Map<Class<?>, Boolean> CONTEXTUAL = new ConcurrentHashMap<>();

    private final BlockStateModel model;
    private final boolean contextual;
    private final ItemTransforms transforms;
    private final boolean gui3d;
    private final boolean blockLight;
    private final ItemOverrides overrides;
    private final @Nullable List<BlockStateModelPart> fixedParts;
    @SuppressWarnings("unchecked")
    private final List<net.minecraft.client.renderer.block.model.BakedQuad>[] fixedQuads =
            new List[(FACES.length + 1) * (Layers.all().length + 1)];
    private volatile @Nullable ChunkRenderTypeSet fixedTypes;
    private volatile @Nullable List<BakedModel> itemPasses;

    public BlockModelAdapter(BlockStateModel model) {
        this(model, ItemTransforms.NO_TRANSFORMS, true, true, ItemOverrides.EMPTY);
    }

    public BlockModelAdapter(
            BlockStateModel model, ItemTransforms transforms, boolean gui3d, boolean blockLight, ItemOverrides overrides) {
        this.model = model;
        this.contextual = isContextual(model);
        this.transforms = transforms;
        this.gui3d = gui3d;
        this.blockLight = blockLight;
        this.overrides = overrides;
        this.fixedParts = model instanceof SingleVariant single ? List.of(single.model()) : null;
    }

    public BlockStateModel model() {
        return model;
    }

    private static boolean isContextual(BlockStateModel model) {
        return CONTEXTUAL.computeIfAbsent(model.getClass(), type -> {
            try {
                return type.getMethod("collectParts", BlockAndTintGetter.class, BlockPos.class, BlockState.class,
                                RandomSource.class, List.class)
                        .getDeclaringClass() != BlockStateModel.class;
            } catch (NoSuchMethodException e) {
                return false;
            }
        });
    }

    private List<BlockStateModelPart> parts(RandomSource random, ModelData data) {
        List<BlockStateModelPart> parts = data.get(PARTS);
        if (parts != null) return parts;
        if (fixedParts != null) return fixedParts;
        parts = new ArrayList<>(2);
        model.collectParts(random, parts);
        return parts;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (!contextual) return modelData;
        List<BlockStateModelPart> parts = new ArrayList<>(2);
        model.collectParts(level, pos, state, RandomSource.create(state.getSeed(pos)), parts);
        return modelData.derive().with(PARTS, parts).build();
    }

    @Override
    public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(
            @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        ChunkSectionLayer layer = renderType == null ? null : Layers.layerOrNull(renderType);
        if (fixedParts != null && !data.has(PARTS)) {
            int index = (side == null ? 0 : side.ordinal() + 1) * (Layers.all().length + 1)
                    + (layer == null ? 0 : layer.ordinal() + 1);
            List<net.minecraft.client.renderer.block.model.BakedQuad> cached = fixedQuads[index];
            if (cached == null) fixedQuads[index] = cached = List.copyOf(collect(fixedParts, side, layer));
            return cached;
        }
        return collect(parts(rand, data), side, layer);
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> collect(
            List<BlockStateModelPart> parts, @Nullable Direction side, @Nullable ChunkSectionLayer layer) {
        List<net.minecraft.client.renderer.block.model.BakedQuad> out = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(side)) {
                if (layer == null || quad.materialInfo().layer() == layer) out.add(quad.toVanilla());
            }
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        if (fixedParts != null && !data.has(PARTS)) {
            ChunkRenderTypeSet set = fixedTypes;
            if (set == null) fixedTypes = set = typesOf(fixedParts);
            return set;
        }
        return typesOf(parts(rand, data));
    }

    private static ChunkRenderTypeSet typesOf(List<BlockStateModelPart> parts) {
        Set<RenderType> types = new LinkedHashSet<>();
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) types.add(Layers.renderType(quad.materialInfo().layer()));
            for (Direction face : FACES)
                for (BakedQuad quad : part.getQuads(face)) types.add(Layers.renderType(quad.materialInfo().layer()));
        }
        return types.isEmpty() ? ChunkRenderTypeSet.none() : ChunkRenderTypeSet.of(types);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        for (BlockStateModelPart part : parts(RandomSource.create(42L), data))
            if (part.useAmbientOcclusion()) return TriState.DEFAULT;
        return TriState.FALSE;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return fixedParts == null || fixedParts.get(0).useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    @Override
    public boolean usesBlockLight() {
        return blockLight;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return model.particleMaterial().sprite();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    // ------------------------------------------------------------------ items

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        List<RenderType> types = itemTypes();
        return types.isEmpty() ? List.of(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()) : types;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        List<BakedModel> passes = itemPasses;
        if (passes == null) {
            List<RenderType> types = itemTypes();
            if (types.size() <= 1) {
                passes = List.of(this);
            } else {
                List<BakedModel> list = new ArrayList<>(types.size());
                for (RenderType type : types) list.add(new ItemPass(this, type));
                passes = List.copyOf(list);
            }
            itemPasses = passes;
        }
        return passes;
    }

    private List<RenderType> itemTypes() {
        Set<RenderType> types = new LinkedHashSet<>();
        for (BlockStateModelPart part : parts(RandomSource.create(42L), ModelData.EMPTY)) {
            for (BakedQuad quad : part.getQuads(null)) types.add(quad.materialInfo().itemRenderType());
            for (Direction face : FACES)
                for (BakedQuad quad : part.getQuads(face)) types.add(quad.materialInfo().itemRenderType());
        }
        return List.copyOf(types);
    }

    /** The quads of one item render type. */
    private static final class ItemPass implements IDynamicBakedModel {
        private final BlockModelAdapter owner;
        private final RenderType type;

        ItemPass(BlockModelAdapter owner, RenderType type) {
            this.owner = owner;
            this.type = type;
        }

        @Override
        public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(
                @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                @Nullable RenderType renderType) {
            List<net.minecraft.client.renderer.block.model.BakedQuad> out = new ArrayList<>();
            for (BlockStateModelPart part : owner.parts(rand, data))
                for (BakedQuad quad : part.getQuads(side))
                    if (quad.materialInfo().itemRenderType() == type) out.add(quad.toVanilla());
            return out;
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return List.of(type);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return owner.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return owner.gui3d;
        }

        @Override
        public boolean usesBlockLight() {
            return owner.blockLight;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return owner.getParticleIcon();
        }

        @Override
        public ItemTransforms getTransforms() {
            return owner.transforms;
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
