// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import com.hbm.backport.client.blockmodel.BlockStateModel;
import com.hbm.backport.client.blockmodel.ModelState;
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

public record PileDeviceModel(BlockPileDevice.Kind kind) implements BlockModel<HFRWavefrontObject> {

    public static final String VENT_FAN = "Fan";
    private static final String[] LOADER_BODY = {"Loader"};
    private static final String[] VENT_PIPE = {"Pipe"};
    private static final String[] CONTROL_BASE = {"Base"};

    public static ResourceLocation objId(BlockPileDevice.Kind kind) {
        return Library.id("models/pile/" + kind.modelName() + ".obj");
    }

    public static ResourceLocation baseModel(BlockPileDevice.Kind kind) {
        return Library.id("block/" + kind.modelName() + "_base");
    }

    private static OctahedralGroup facingRotation(Direction facing) {
        return Facing.yawRotation(facing, 90);
    }

    private static List<ObjUnbakedGeometry.Group> groups(
            HFRWavefrontObject obj,
            Material.Baked material,
            BlockPileDevice.Kind kind,
            Direction facing) {
        OctahedralGroup yaw = facingRotation(facing);
        List<ObjUnbakedGeometry.Group> groups = new ArrayList<>(2);
        switch (kind) {
            case LOADER -> {
                groups.add(
                        ObjUnbakedGeometry.Group.opaque(
                                obj, LOADER_BODY, material, posed(yaw, 0F, 0F, 0F)));
            }
            case VENT ->
                    groups.add(
                            ObjUnbakedGeometry.Group.opaque(
                                    obj, VENT_PIPE, material, posed(yaw, 0F, 0F, 0F)));
            case CONTROL -> {
                groups.add(
                        ObjUnbakedGeometry.Group.opaque(
                                obj, CONTROL_BASE, material, posed(yaw, 0F, 0F, 0F)));
            }
        }
        return groups;
    }

    private static ModelState posed(OctahedralGroup rotation, float tx, float ty, float tz) {
        Matrix4f matrix =
                new Matrix4f().translation(tx, ty, tz).mul(new Matrix4f(com.hbm.backport.Octahedral.matrix(rotation)));
        return Boxes.pose(new Transformation(matrix));
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(objId(kind));
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(baseModel(kind), kind, prepared, state);
    }

    public record Root(
            ResourceLocation carrier, BlockPileDevice.Kind kind, HFRWavefrontObject obj, BlockState state)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Direction facing = state.getValue(BlockPileDevice.FACING);
            return ObjUnbakedGeometry.bakeGroups(groups(obj, material, kind, facing), 0F, 0F, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return state.getValue(BlockPileDevice.FACING);
        }
    }
}
