// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineMixer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.core.BlockPos;
import com.hbm.backport.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderMixer
        implements BlockEntityRenderer<BlockEntityMachineMixer, RenderMixer.State>,
                ConcurrentRenderStateExtraction {
    private static final int MIXER = ResourceManager.mixer.partId("Mixer");
    private static final int FLUID = ResourceManager.mixer.partId("Fluid");
    private static final int NO_TYPE_COLOR = 0x888888;

    private final HFRWavefrontObject model;
    private final RenderType baseType;
    private final RenderType fluidType;

    public RenderMixer() {
        this.model = ResourceManager.mixer;
        this.baseType = WorldRenderPipeline.oneSidedCutout(ResourceManager.mixer_tex);
        this.fluidType = FlatTranslucent.lit(ResourceManager.white_tex);
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineMixer be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1);
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineMixer be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.rotation = Mth.lerp(partialTicks, be.prevRotation, be.rotation);

        int totalFill = 0, totalMax = 0;
        for (FluidTankNTM tank : be.tanks) {
            if (tank.getTankType() != null) {
                totalFill += tank.getFill();
                totalMax += tank.getMaxFill();
            }
        }
        state.fluidFrac = totalMax > 0 ? (double) totalFill / totalMax : 0D;

        NTMFluidProperty prop =
                NTMFluidProperties.get(be.tanks[BlockEntityMachineMixer.TANK_OUT].getFluid());
        state.fluidColor = ARGB.color(0xBF, prop != null ? prop.colorARGB() : NO_TYPE_COLOR);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        ps.pushPose();
        ps.mulPose(Axis.YN.rotationDegrees(s.rotation));
        part(col, ps, MIXER, light);
        ps.popPose();

        if (s.fluidFrac > 0) {
            ps.pushPose();
            ps.translate(0, 1, 0);
            ps.scale(1F, (float) (s.fluidFrac * 0.99), 1F);
            ps.translate(0, -1, 0);
            int color = s.fluidColor;
            col.submitCustomGeometry(
                    ps,
                    fluidType,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, color, FLUID));
            ps.popPose();
        }

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int name, int light) {
        col.submitCustomGeometry(
                ps, baseType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float rotation;
        public double fluidFrac;
        public int fluidColor;
    }
}
