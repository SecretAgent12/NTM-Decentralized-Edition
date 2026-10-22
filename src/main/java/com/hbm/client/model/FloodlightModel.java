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
import com.hbm.backport.Octahedral;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record FloodlightModel() implements BlockModel<HFRWavefrontObject> {

    private static final ResourceLocation OBJ = Library.id("models/blocks/floodlight.obj");
    private static final String[] BASE = {"Base"};

    private static OctahedralGroup rotationFor(int meta) {
        return switch (meta) {
            case 0 -> Octahedral.blockRot('x', 180);
            case 1 -> OctahedralGroup.IDENTITY;
            // backport-fix: BF-107 (GitHub #26, PR #31 by noli-visko) the base on the south (2) and north (3) walls:
            // 1.21.1's OctahedralGroup.compose takes its operands in the other order than 26.x (as BF-079/080), so
            // ntm-next's X_90.compose(Y_90) came out wrong. Its rotations multiplied in the right order match the
            // 1.7.10 RenderFloodlight (glRotate x 90, z 180, y 90 / x 90, y 90), checked by matrices for all 8 metas
            case 2 -> Octahedral.product(Octahedral.blockRot('x', 90), Octahedral.blockRot('y', 90));
            case 3 -> Octahedral.product(Octahedral.blockRot('x', 270), Octahedral.blockRot('y', 270));
            case 4 -> Octahedral.blockRot('z', 270);
            case 5 -> Octahedral.blockRot('x', 180).compose(Octahedral.blockRot('z', 90));
            case 6 -> Octahedral.blockRot('x', 180).compose(Octahedral.blockRot('y', 270));
            case 7 -> Octahedral.blockRot('y', 270);
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
