// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.generic.BlockSteelScaffold;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.hbm.backport.client.blockmodel.BlockStateModel;
import com.hbm.backport.client.blockmodel.ModelBaker;
import com.hbm.backport.client.blockmodel.ResolvedModel;
import com.hbm.backport.client.blockmodel.QuadCollection;
import com.hbm.backport.client.blockmodel.Material;
import com.hbm.backport.client.blockmodel.TextureSlots;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record ScaffoldModel() implements BlockModel<HFRWavefrontObject> {

    private static final ResourceLocation OBJ = Library.id("models/blocks/scaffold.obj");
    private static final String[] SCAFFOLD = {"Cube_Cube.001"};

    private static final Map<BlockSteelScaffold.Orient, Matrix4fc> MATRICES = buildMatrices();

    private static Map<BlockSteelScaffold.Orient, Matrix4fc> buildMatrices() {
        Map<BlockSteelScaffold.Orient, Matrix4fc> m =
                new EnumMap<>(BlockSteelScaffold.Orient.class);
        m.put(BlockSteelScaffold.Orient.NS_UPRIGHT, matrix(-90F, 0F, 0.5F, 0.0F, 0.5F));
        m.put(BlockSteelScaffold.Orient.EW_FLAT, matrix(-90F, 90F, 0.5F, 0.5F, 0.0F));
        m.put(BlockSteelScaffold.Orient.EW_UPRIGHT, matrix(-180F, 0F, 0.5F, 0.0F, 0.5F));
        m.put(BlockSteelScaffold.Orient.NS_FLAT, matrix(-180F, 90F, 1.0F, 0.5F, 0.5F));
        return m;
    }

    private static Matrix4fc matrix(float yawDeg, float pitchDeg, float ox, float oy, float oz) {
        return new Matrix4f()
                .translate(ox, oy, oz)
                .rotateY((float) Math.toRadians(yawDeg))
                .rotateZ((float) Math.toRadians(-pitchDeg));
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block), prepared, state.getValue(BlockSteelScaffold.ORIENT));
    }

    public record Root(ResourceLocation carrier, HFRWavefrontObject obj, BlockSteelScaffold.Orient orient)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(
                            ObjUnbakedGeometry.Group.raw(
                                    obj, SCAFFOLD, material, MATRICES.get(orient))),
                    0F,
                    ObjUnbakedGeometry.RAW_OFFSET_Y,
                    0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return orient;
        }
    }
}
