// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.FluidPipeAnchorBlock;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.math.OctahedralGroup;
import java.util.List;
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

public record AnchorModel() implements BlockModel<HFRWavefrontObject> {

    private static final ResourceLocation OBJ = Library.id("models/network/pipe_anchor.obj");
    private static final String[] ANCHOR = {"Anchor"};

    private static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> Facing.rx(180);
            case UP -> OctahedralGroup.IDENTITY;
            case NORTH -> Facing.rx(90).compose(Facing.rz(180));
            case SOUTH -> Facing.rx(90);
            case WEST -> Facing.rx(90).compose(Facing.rz(90));
            case EAST -> Facing.rx(90).compose(Facing.rz(270));
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), prepared, state);
    }

    public record Root(ResourceLocation carrier, HFRWavefrontObject obj, BlockState state)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Direction facing = state.getValue(FluidPipeAnchorBlock.FACING);
            List<ObjUnbakedGeometry.Group> groups =
                    List.of(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj,
                                    ANCHOR,
                                    material,
                                    BlockModelRotation.get(rotationFor(facing))));
            return ObjUnbakedGeometry.bakeGroups(groups, 0F, 0F, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(FluidPipeAnchorBlock.FACING);
        }
    }
}
