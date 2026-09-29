// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.handler.MissileStruct;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.bomb.BlockEntityCompactLauncher;
import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderCompactLauncher
        implements BlockEntityRenderer<BlockEntityCompactLauncher, RenderCompactLauncher.State>,
                ConcurrentRenderStateExtraction {

    private static final double MISSILE_Y = 1.0625D;

    private static final RenderType BODY =
            RenderTypes.entityCutoutCull(ResourceManager.compact_launcher_tex);

    private final MissilePronter pronter = new MissilePronter();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityCompactLauncher launcher) {
        return AABB.INFINITE;
    }

    @Override
    public void extractRenderState(
            BlockEntityCompactLauncher launcher,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                launcher, state, partialTicks, camera, breakProgress);
        state.parts = launcher.loadedMissile;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        collector.submitCustomGeometry(
                poseStack,
                BODY,
                (pose, buffer) -> ResourceManager.compact_launcher.render(pose, buffer, light, -1));
        poseStack.translate(0D, MISSILE_Y, 0D);
        pronter.pront(state.parts, poseStack, collector, light);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        MissileStruct parts = MissileStruct.EMPTY;
    }
}
