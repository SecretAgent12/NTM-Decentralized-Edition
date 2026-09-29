// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.machine.Floodlight;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.math.OctahedralGroup;
import java.util.List;
import com.hbm.backport.client.blockmodel.BlockModelRotation;
import com.hbm.backport.client.blockmodel.BlockStateModel;
import com.hbm.backport.client.blockmodel.ModelBaker;
import com.hbm.backport.client.blockmodel.ResolvedModel;
import com.hbm.backport.client.blockmodel.QuadCollection;
import com.hbm.backport.client.blockmodel.Material;
import com.hbm.backport.client.blockmodel.TextureSlots;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record FloodlightModel() implements BlockModel<HFRWavefrontObject> {

    private static final ResourceLocation OBJ = Library.id("models/blocks/floodlight.obj");
    private static final String[] BASE = {"Base"};

    private static OctahedralGroup rotationFor(int meta) {
        return switch (meta) {
            case 0 -> com.hbm.backport.Octahedral.blockRot('x', 180);
            case 1 -> OctahedralGroup.IDENTITY;
            case 2 -> com.hbm.backport.Octahedral.blockRot('x', 90).compose(com.hbm.backport.Octahedral.blockRot('y', 90));
            case 3 -> com.hbm.backport.Octahedral.blockRot('x', 270).compose(com.hbm.backport.Octahedral.blockRot('y', 270));
            case 4 -> com.hbm.backport.Octahedral.blockRot('z', 270);
            case 5 -> com.hbm.backport.Octahedral.blockRot('x', 180).compose(com.hbm.backport.Octahedral.blockRot('z', 90));
            case 6 -> com.hbm.backport.Octahedral.blockRot('x', 180).compose(com.hbm.backport.Octahedral.blockRot('y', 270));
            case 7 -> com.hbm.backport.Octahedral.blockRot('y', 270);
            default -> throw new IllegalStateException("Unexpected floodlight facing: " + meta);
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
            int facing = state.getValue(Floodlight.FACING);
            List<ObjUnbakedGeometry.Group> groups =
                    List.of(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj,
                                    BASE,
                                    material,
                                    BlockModelRotation.get(rotationFor(facing))));
            return ObjUnbakedGeometry.bakeGroups(groups, 0F, 0F, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(Floodlight.FACING);
        }
    }
}
