// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blocks.bomb.BlockLandmineAP;
import com.hbm.lib.Library;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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
import org.jspecify.annotations.Nullable;

public record MineModel(Kind kind) implements BlockModel<HFRWavefrontObject> {

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(kind.obj);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block),
                prepared,
                kind.matrix,
                kind.slot.apply(state),
                kind.shading,
                kind.overrides);
    }

    private static ObjUnbakedGeometry.Group flat(
            HFRWavefrontObject model,
            String @Nullable [] parts,
            Material.Baked material,
            Matrix4fc blockSpace) {
        List<GroupObject> groups = new ArrayList<>(model.groups.length);
        for (GroupObject group : model.groups) groups.add(group.flatShaded());
        return ObjUnbakedGeometry.Group.rawSmooth(
                new HFRWavefrontObject(model.source, groups), parts, material, blockSpace);
    }

    public enum Kind {
        AP(
                "models/blocks/ap_mine.obj",
                new Matrix4f()
                        .translate(0.5F, 0F, 0.5F)
                        .rotateY((float) Math.toRadians(180))
                        .scale(0.375F)
                        .translate(0F, -0.21875F, 0F),
                state -> state.getValue(BlockLandmineAP.GROUND).getSerializedName(),
                MineModel::flat,
                "Circle"),

        HE(
                "models/blocks/marelet.obj",
                new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY((float) Math.toRadians(90)),
                state -> ObjUnbakedGeometry.SINGLE_SLOT,
                ObjUnbakedGeometry.Group::rawSmooth,
                "Circle"),

        SHRAP(
                "models/blocks/ap_mine.obj",
                new Matrix4f()
                        .translate(0.5F, 0F, 0.5F)
                        .rotateY((float) Math.toRadians(180))
                        .scale(0.375F)
                        .translate(0F, -0.21875F, 0F),
                state -> ObjUnbakedGeometry.SINGLE_SLOT,
                MineModel::flat,
                "Circle"),

        FAT(
                "models/blocks/mine_fat.obj",
                new Matrix4f()
                        .translate(0.5F, 0F, 0.5F)
                        .rotateY((float) Math.toRadians(180))
                        .scale(0.25F),
                state -> ObjUnbakedGeometry.SINGLE_SLOT,
                ObjUnbakedGeometry.Group::rawCutout,
                "Sphere"),

        NAVAL(
                "models/blocks/naval_mine.obj",
                new Matrix4f()
                        .translate(0.5F, 0F, 0.5F)
                        .rotateY((float) Math.toRadians(180))
                        .translate(0F, 0.5F, 0F),
                state -> ObjUnbakedGeometry.SINGLE_SLOT,
                ObjUnbakedGeometry.Group::rawSmooth,
                "Cylinder");

        public final ResourceLocation obj;
        public final Matrix4fc matrix;
        public final Function<BlockState, String> slot;

        public final Shading shading;
        public final ObjUnbakedGeometry.Overrides overrides;

        Kind(
                String path,
                Matrix4fc matrix,
                Function<BlockState, String> slot,
                Shading shading,
                String... doubleSided) {
            this.obj = Library.id(path);
            this.matrix = matrix;
            this.slot = slot;
            this.shading = shading;
            this.overrides = new ObjUnbakedGeometry.Overrides(null, null, null, doubleSided);
        }
    }

    @FunctionalInterface
    public interface Shading {
        ObjUnbakedGeometry.Group group(
                HFRWavefrontObject model,
                String @Nullable [] parts,
                Material.Baked material,
                Matrix4fc blockSpace);
    }

    public record Root(
            ResourceLocation carrier,
            HFRWavefrontObject obj,
            Matrix4fc matrix,
            String slot,
            Shading shading,
            ObjUnbakedGeometry.Overrides overrides)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material = BlockModel.slot(baker, carrier, slots, slot);
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(shading.group(obj, null, material, matrix)),
                    List.of(),
                    0F,
                    ObjUnbakedGeometry.RAW_OFFSET_Y,
                    0F,
                    overrides);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return this;
        }
    }
}
