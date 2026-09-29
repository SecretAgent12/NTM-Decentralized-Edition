// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.albion.BlockPABeamline;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.albion.BlockEntityPABeamline;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import com.hbm.backport.ARGB;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPABeamline
        implements BlockEntityRenderer<BlockEntityPABeamline, RenderPABeamline.State>,
                ConcurrentRenderStateExtraction {
    private static final int BEAMLINE_GLASS = ResourceManager.pa_beamline.partId("BeamlineGlass");

    private static final ResourceLocation WHITE = RenderTextures.WHITE;

    private final HFRWavefrontObject model;
    private final RenderType glassType;

    public RenderPABeamline(BlockEntityRendererProvider.Context context) {
        this.model = ResourceManager.pa_beamline;

        this.glassType = FlatCutout.of(WHITE);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityPABeamline be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 1,
                pos.getZ() + 2);
    }

    @Override
    public void extractRenderState(
            BlockEntityPABeamline be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.window = be.getBlockState().getValue(BlockPABeamline.WINDOW);
        state.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.flash = be.flash(partialTicks);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.window || s.flash <= 0F) return;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        int argb = ARGB.colorFromFloat(1F, 0.9F * s.flash, 0.9F * s.flash, s.flash);
        col.submitCustomGeometry(
                ps,
                glassType,
                (pose, buffer) ->
                        model.renderPart(
                                pose, buffer, LightTexture.FULL_BRIGHT, argb, BEAMLINE_GLASS));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public boolean window;
        public float yaw;
        public float flash;
    }
}
