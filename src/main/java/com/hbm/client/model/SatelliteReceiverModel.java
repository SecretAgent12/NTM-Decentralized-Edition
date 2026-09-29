// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import com.hbm.backport.client.blockmodel.BlockStateModel;
import com.hbm.backport.client.blockmodel.ModelBaker;
import com.hbm.backport.client.blockmodel.ResolvedModel;
import com.hbm.backport.client.blockmodel.QuadCollection;
import com.hbm.backport.client.blockmodel.Material;
import com.hbm.backport.client.blockmodel.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record SatelliteReceiverModel() implements BlockModel<HFRWavefrontObject> {

    private static final Shape[] SHAPES = {
        new Shape(0, 0, 0F, 0F, 0F, 12F, 16F, 12F, -6F, 8F, -6F, 0F, 0F, 0F),
        new Shape(10, 28, 3F, 9F, -8F, 8F, 8F, 2F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 39, 3F, 7F, -10F, 8F, 2F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 28, 1F, 9F, -10F, 2F, 8F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 28, 11F, 9F, -10F, 2F, 8F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 39, 3F, 17F, -10F, 8F, 2F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 44, 6F, 12F, -11F, 2F, 2F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 49, 6.5F, 12.5F, -14F, 1F, 1F, 3F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
        new Shape(0, 53, 6F, 12F, -16F, 2F, 2F, 2F, -3F, 6F, 0F, -0.2617994F, -0.4363323F, 0F),
    };

    private static String[] groupName(int shape) {
        return new String[] {"shape" + shape};
    }

    // backport: 1.21.1 keeps ModelPart.Cube#polygons and its Polygon/Vertex types private; the
    // same corners are read back through the public Cube#compile (which emits every polygon's
    // four vertices in order, positions divided by 16 and UVs already normalised)
    private static float[] corners(ModelPart.Cube cube) {
        com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
        it.unimi.dsi.fastutil.floats.FloatArrayList out = new it.unimi.dsi.fastutil.floats.FloatArrayList();
        cube.compile(
                pose.last(),
                new com.mojang.blaze3d.vertex.VertexConsumer() {
                    @Override
                    public void addVertex(
                            float x, float y, float z, int color, float u, float v, int overlay,
                            int light, float nx, float ny, float nz) {
                        out.add(x * 16F);
                        out.add(y * 16F);
                        out.add(z * 16F);
                        out.add(u);
                        out.add(v);
                    }

                    @Override
                    public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) {
                        return this;
                    }

                    @Override
                    public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
                        return this;
                    }

                    @Override
                    public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
                        return this;
                    }

                    @Override
                    public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
                        return this;
                    }

                    @Override
                    public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
                        return this;
                    }
                },
                0,
                0,
                -1);
        return out.toFloatArray();
    }

    public static float yawFor(Direction facing) {
        return Facing.yawCcw(facing, 0);
    }

    public static Matrix4fc matrixFor(Direction facing) {
        return new Matrix4f()
                .translate(0.5F, 1.5F, 0.5F)
                .rotateZ((float) Math.toRadians(180))
                .rotateY((float) Math.toRadians(yawFor(facing)));
    }

    @Override
    public HFRWavefrontObject prepare() {
        List<GroupObject> groups = new ArrayList<>(SHAPES.length);
        for (int i = 0; i < SHAPES.length; i++) {
            Shape shape = SHAPES[i];
            ModelPart.Cube cube =
                    new ModelPart.Cube(
                            shape.u(),
                            shape.v(),
                            shape.x(),
                            shape.y(),
                            shape.z(),
                            shape.w(),
                            shape.h(),
                            shape.d(),
                            0F,
                            0F,
                            0F,
                            true,
                            64F,
                            64F,
                            EnumSet.allOf(Direction.class));

            groups.add(GroupObject.ofPosUv("shape" + i, corners(cube)));
        }
        return new HFRWavefrontObject("satellite_receiver boxes", groups);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(
                BlockModel.base(block),
                prepared,
                state.getValue(HorizontalDirectionalBlock.FACING));
    }

    private record Shape(
            int u,
            int v,
            float x,
            float y,
            float z,
            float w,
            float h,
            float d,
            float pivotX,
            float pivotY,
            float pivotZ,
            float rotX,
            float rotY,
            float rotZ) {}

    public record Root(ResourceLocation carrier, HFRWavefrontObject mesh, Direction facing)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Matrix4fc facingMatrix = matrixFor(facing);

            List<ObjUnbakedGeometry.Group> groups = new ArrayList<>(SHAPES.length);
            for (int i = 0; i < SHAPES.length; i++) {
                Shape shape = SHAPES[i];
                Matrix4f matrix =
                        new Matrix4f(facingMatrix)
                                .scale(0.0625F)
                                .translate(shape.pivotX(), shape.pivotY(), shape.pivotZ())
                                .rotateZ(shape.rotZ())
                                .rotateY(shape.rotY())
                                .rotateX(shape.rotX());
                groups.add(ObjUnbakedGeometry.Group.raw(mesh, groupName(i), material, matrix));
            }
            return ObjUnbakedGeometry.bakeGroups(groups, 0F, ObjUnbakedGeometry.RAW_OFFSET_Y, 0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return facing;
        }
    }
}
