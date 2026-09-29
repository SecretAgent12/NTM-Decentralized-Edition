// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.machine.PistonInserter;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.math.OctahedralGroup;
import com.hbm.backport.client.blockmodel.BlockModelRotation;
import com.hbm.backport.client.blockmodel.BlockStateModel;
import com.hbm.backport.client.blockmodel.ModelBaker;
import com.hbm.backport.client.blockmodel.ResolvedModel;
import com.hbm.backport.client.blockmodel.QuadCollection;
import com.hbm.backport.client.blockmodel.Material;
import com.hbm.backport.client.blockmodel.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record PistonInserterModel() implements BlockModel<HFRWavefrontObject> {

    public static final ResourceLocation OBJ = Library.id("models/machines/piston_inserter.obj");
    public static final ResourceLocation BASE_MODEL = Library.id("block/piston_inserter_base");

    public static final String PISTON = "Piston";
    private static final String[] FRAME = {"Frame"};

    private static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> OctahedralGroup.ROT_180_FACE_YZ;
            case UP -> OctahedralGroup.IDENTITY;
            case NORTH -> OctahedralGroup.ROT_180_EDGE_YZ_NEG;
            case SOUTH -> OctahedralGroup.ROT_90_X_POS;
            case WEST -> OctahedralGroup.ROT_120_PNP;
            case EAST -> OctahedralGroup.ROT_120_PPN;
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(prepared, state);
    }

    public record Root(HFRWavefrontObject obj, BlockState state) implements CarrierRoot {

        @Override
        public ResourceLocation carrier() {
            return BASE_MODEL;
        }

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Direction facing = state.getValue(PistonInserter.FACING);
            return ObjUnbakedGeometry.bakeGroups(
                    obj,
                    FRAME,
                    material,
                    BlockModelRotation.get(rotationFor(facing)),
                    0F,
                    0F,
                    0F,
                    false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(PistonInserter.FACING);
        }
    }
}
