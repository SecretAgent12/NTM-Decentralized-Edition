// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.network.RadioTorchBlock;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record RadioTorchModel() implements BlockModel<HFRWavefrontObject> {

    static final ResourceLocation OBJ = Library.id("models/blocks/rtty.obj");

    public static ResourceLocation baseModel(RadioTorchBlock.Kind kind, BlockState state) {
        String texture =
                switch (kind) {
                    case SENDER ->
                            state.getValue(RadioTorchBlock.LIT)
                                    ? "rtty_sender_on"
                                    : "rtty_sender_off";
                    case RECEIVER ->
                            state.getValue(RadioTorchBlock.LIT) ? "rtty_rec_on" : "rtty_rec_off";
                    case LOGIC ->
                            state.getValue(RadioTorchBlock.LIT)
                                    ? "rtty_logic_on"
                                    : "rtty_logic_off";
                    case COUNTER -> "rtty_counter";
                    case READER -> "rtty_reader";
                    case CONTROLLER -> "rtty_controller";
                };
        return Library.id("block/" + texture + "_base");
    }

    static OctahedralGroup rotationFor(Direction facing) {
        return switch (facing) {
            case DOWN -> com.hbm.backport.Octahedral.blockRot('z', 180);
            case UP -> OctahedralGroup.IDENTITY;
            case NORTH -> com.hbm.backport.Octahedral.blockRot('y', 90).compose(com.hbm.backport.Octahedral.blockRot('z', 90));
            case SOUTH -> com.hbm.backport.Octahedral.blockRot('y', 270).compose(com.hbm.backport.Octahedral.blockRot('z', 90));
            case WEST -> com.hbm.backport.Octahedral.blockRot('y', 180).compose(com.hbm.backport.Octahedral.blockRot('z', 90));
            case EAST -> com.hbm.backport.Octahedral.blockRot('z', 90);
        };
    }

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.faceNormals(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(baseModel(((RadioTorchBlock) block).kind(), state), prepared, state);
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

            return ObjUnbakedGeometry.bakeGroups(
                    obj,
                    null,
                    material,
                    BlockModelRotation.get(rotationFor(state.getValue(RadioTorchBlock.FACING))),
                    0F,
                    0.5F,
                    0F,
                    false,
                    false);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return List.of(carrier, state.getValue(RadioTorchBlock.FACING));
        }
    }
}
