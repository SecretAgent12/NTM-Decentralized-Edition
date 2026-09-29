// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.TileEntityLantern;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.core.BlockPos;
import com.hbm.backport.ARGB;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderLantern
        implements BlockEntityRenderer<TileEntityLantern, RenderLantern.State>,
                ConcurrentRenderStateExtraction {

    public static final int LANTERN_PART = ResourceManager.lantern.partId("Lantern");
    public static final int LIGHT_PART = ResourceManager.lantern.partId("Light");

    private static final RenderType LIGHT_TYPE = FlatCutout.of(ResourceManager.white_tex);

    public static int flicker(long millis) {
        float mult = (float) (Math.sin(millis / 200D) / 2 + 0.5) * 0.1F + 0.9F;
        return ARGB.colorFromFloat(1F, mult, mult, 0.7F * mult);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(TileEntityLantern be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 6, pos.getZ() + 1);
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
    public void extractRenderState(
            TileEntityLantern be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.color = flicker(GameTime.now());
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        int color = s.color;
        col.submitCustomGeometry(
                ps,
                LIGHT_TYPE,
                (pose, buffer) ->
                        ResourceManager.lantern.renderPart(
                                pose, buffer, LightTexture.FULL_BRIGHT, color, LIGHT_PART));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        int color;
    }
}
