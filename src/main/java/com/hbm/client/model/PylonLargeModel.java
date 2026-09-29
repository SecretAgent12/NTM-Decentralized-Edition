// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.backport.client.blockmodel.Transparency;
import com.mojang.math.Transformation;
import java.util.List;
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
import org.joml.Matrix4f;

public record PylonLargeModel() implements BlockModel<HFRWavefrontObject> {

    public static final ResourceLocation MESH = Library.id("models/network/pylon_large.obj");

    private static final String[] PARTS = {"Cube_Cube.001"};
    private static final ObjUnbakedGeometry.Overrides NO_CULL =
            new ObjUnbakedGeometry.Overrides(null, null, null, PARTS);

    private static float yawFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 90F;
            case WEST -> 135F;
            case EAST -> 45F;
            default -> 0F;
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(MESH);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block), prepared, state.getValue(BlockMultiblockCore.FACING));
    }

    public record Root(ResourceLocation carrier, HFRWavefrontObject obj, Direction facing)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            ObjUnbakedGeometry.Group group =
                    new ObjUnbakedGeometry.Group(
                            obj,
                            PARTS,
                            material,
                            Boxes.pose(
                                    new Transformation(
                                            new Matrix4f()
                                                    .rotationY(
                                                            (float)
                                                                    Math.toRadians(
                                                                            yawFor(facing))))),
                            Transparency.NONE,
                            false);
            return ObjUnbakedGeometry.bakeGroups(List.of(group), List.of(), 0F, 0F, 0F, NO_CULL);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return facing;
        }
    }
}
