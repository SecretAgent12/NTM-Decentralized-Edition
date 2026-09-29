// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.machine.MachineRTG;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import java.util.ArrayList;
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

public record RTGModel() implements BlockModel<HFRWavefrontObject> {

    private static final ResourceLocation OBJ = Library.id("models/machines/rtg.obj");
    private static final String[] GEN = {"Gen"};
    private static final String[] CONNECTOR = {"Connector"};
    private static final ObjUnbakedGeometry.Overrides NO_CULL =
            new ObjUnbakedGeometry.Overrides(null, null, null, new String[] {"Gen", "Connector"});

    private static final float BODY_YAW = 180F;
    private static final int CONNECTOR_NORTH_YAW = 270;

    private static ObjUnbakedGeometry.Group group(
            HFRWavefrontObject obj, String[] parts, Material.Baked material, float yaw) {
        return ObjUnbakedGeometry.Group.rawSmooth(
                obj,
                parts,
                material,
                new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY((float) Math.toRadians(yaw)));
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), prepared, MachineRTG.mask(state));
    }

    public record Root(ResourceLocation carrier, HFRWavefrontObject obj, int mask)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            List<ObjUnbakedGeometry.Group> groups = new ArrayList<>(1 + Integer.bitCount(mask));
            groups.add(group(obj, GEN, material, BODY_YAW));
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if ((mask & (1 << dir.ordinal())) == 0) continue;
                groups.add(group(obj, CONNECTOR, material, Facing.yaw(dir, CONNECTOR_NORTH_YAW)));
            }
            return ObjUnbakedGeometry.bakeGroups(
                    groups, List.of(), 0F, ObjUnbakedGeometry.RAW_OFFSET_Y, 0F, NO_CULL);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return mask;
        }
    }
}
