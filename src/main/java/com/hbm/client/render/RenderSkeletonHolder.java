// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockSkeletonHolder;
import com.hbm.tileentity.BlockEntitySkeletonHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import com.hbm.backport.client.itemmodel.ItemModelResolver;
import com.hbm.backport.client.itemmodel.ItemStackRenderState;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSkeletonHolder
        implements BlockEntityRenderer<BlockEntitySkeletonHolder, RenderSkeletonHolder.State>,
                ConcurrentRenderStateExtraction {

    private final ItemModelResolver itemModelResolver;

    public RenderSkeletonHolder(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = com.hbm.backport.client.itemmodel.ItemModelResolver.get();
    }

    private static float renderAngle(int facing) {
        return switch (facing) {
            case 2 -> 180F;
            case 4 -> 270F;
            case 5 -> 90F;
            default -> 0F;
        };
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntitySkeletonHolder be) {
        return new AABB(be.getBlockPos()).inflate(1.0D);
    }

    @Override
    public void extractRenderState(
            BlockEntitySkeletonHolder pedestal,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pedestal, state, partialTicks, cameraPosition, breakProgress);
        state.facing = pedestal.getBlockState().getValue(BlockSkeletonHolder.FACING);
        if (pedestal.item.isEmpty()) {
            state.item = null;
            return;
        }
        if (state.item == null) state.item = new ItemStackRenderState();
        FramedItem.Arm arm =
                FramedItem.resolve(
                        itemModelResolver, state.item, pedestal.item, pedestal.getLevel(), null, 0);
        state.mesh = arm.mesh();
        state.meshLift = arm.groundLift();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.item == null) return;

        poseStack.pushPose();

        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(renderAngle(state.facing)));

        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        if (state.mesh) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
            poseStack.translate(0.0, 0.125, 0.0);
            poseStack.translate(0.0, state.meshLift, 0.0);
        } else if (state.item.usesBlockLight()) {

            poseStack.translate(0.0, 0.125, 0.0);
            poseStack.translate(0.0, FramedItem.FRAMED_BOB, 0.0);
            poseStack.scale(1.25F, 1.25F, 1.25F);
            poseStack.translate(0.0, 0.05F, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.scale(0.25F, 0.25F, 0.25F);
        } else {
            poseStack.scale(1.5F, 1.5F, 1.5F);
            poseStack.translate(0.0, 0.125, 0.0);
            poseStack.translate(0.0, FramedItem.FRAMED_BOB, 0.0);
            poseStack.scale(
                    FramedItem.FRAMED_SPRITE_SCALE,
                    FramedItem.FRAMED_SPRITE_SCALE,
                    FramedItem.FRAMED_SPRITE_SCALE);
            poseStack.translate(0.0, -0.05, 0.0);
        }
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public int facing = 3;
        public @Nullable ItemStackRenderState item;
        public boolean mesh;
        public float meshLift;
    }
}
