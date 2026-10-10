// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.ConnectorBlock;
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

public record ConnectorModel(ResourceLocation obj) implements BlockModel<HFRWavefrontObject> {

    private static final String[] PARTS = {"Cube_Cube.001"};

    private static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> Facing.rx(180);
            case UP -> OctahedralGroup.IDENTITY;
            case NORTH -> Facing.rx(90).compose(Facing.rz(180));
            case SOUTH -> Facing.rx(90);
            // backport-fix: BF-080 on the west and east walls ntm-next's rotation came out turned
            // 90 degrees anticlockwise on 1.21.1 (seen in game, like BF-079): a quarter turn
            // clockwise seen from above is added - ry 270 here (ry 90 turned it the wrong way in
            // game), multiplied out in Octahedral.product. North, south, floor and ceiling were right
            case WEST ->
                    com.hbm.backport.Octahedral.product(
                            Facing.ry(270), Facing.rx(90).compose(Facing.rz(90)));
            case EAST ->
                    com.hbm.backport.Octahedral.product(
                            Facing.ry(270), Facing.rx(90).compose(Facing.rz(270)));
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(obj);
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
            Direction facing = state.getValue(ConnectorBlock.FACING);
            List<ObjUnbakedGeometry.Group> groups =
                    List.of(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj,
                                    PARTS,
                                    material,
                                    BlockModelRotation.get(rotationFor(facing))));
            return ObjUnbakedGeometry.bakeGroups(groups, 0F, 0F, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(ConnectorBlock.FACING);
        }
    }
}
