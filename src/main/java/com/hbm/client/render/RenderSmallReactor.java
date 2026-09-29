// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityReactorResearch;
import com.hbm.backport.client.rendertype.PrimitiveTopology;
import com.hbm.backport.client.rendertype.BlendFunction;
import com.hbm.backport.client.rendertype.ColorTargetState;
import com.hbm.backport.client.rendertype.DepthStencilState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.rendertype.BindGroupLayouts;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import com.hbm.backport.client.rendertype.RenderSetup;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.core.BlockPos;
import com.hbm.backport.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSmallReactor
        implements BlockEntityRenderer<BlockEntityReactorResearch, RenderSmallReactor.State>,
                ConcurrentRenderStateExtraction {

    private static final float BODY_YAW = 180F;

    private static final double GLOW_MIN = 0.285D, GLOW_MAX = 0.7D, GLOW_STEP = 0.025D;
    private static final double GLOW_TOP = 1.375D, GLOW_BOTTOM = 1.375D;
    private static final int GLOW_COLOR = 0x66E5FF;

    public static final int GLOW_FLUX = 10;

    static final RenderPipeline GLOW_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_reactor_glow")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.NEW_ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(DepthStencilState.DEFAULT)
                            .withCull(false));
    private static final RenderType GLOW =
            com.hbm.backport.client.rendertype.RenderTypeFactory.create(
                    "ntm_reactor_glow",
                    RenderSetup.builder(GLOW_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());

    private final RenderType rodsType;

    public RenderSmallReactor() {
        this.rodsType = WorldRenderPipeline.oneSidedCutout(ResourceManager.reactor_small_rods_tex);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buf,
            int argb,
            int light,
            double x,
            double y,
            double z) {
        Vertices.emit(buf, pose, (float) x, (float) y, (float) z, argb, 0F, 0F, light, 0F, 0F, 1F);
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
    public AABB getRenderBoundingBox(BlockEntityReactorResearch be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 5,
                pos.getZ() + 2);
    }

    @Override
    public void extractRenderState(
            BlockEntityReactorResearch be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.controlLevel =
                Mth.lerp(partialTicks, (float) be.lastControlLevel, (float) be.controlLevel);
        state.totalFlux = be.totalFlux;
        state.glowing = be.totalFlux > GLOW_FLUX && be.isSubmerged();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(BODY_YAW));

        ps.pushPose();
        ps.translate(0.0, s.controlLevel, 0.0);
        col.submitCustomGeometry(
                ps,
                rodsType,
                (pose, buf) -> ResourceManager.reactor_small_rods.render(pose, buf, light, -1));
        ps.popPose();

        if (s.glowing) {
            final int flux = s.totalFlux;
            col.submitCustomGeometry(
                    ps,
                    GLOW,
                    (pose, buf) -> {
                        for (double d = GLOW_MIN; d < GLOW_MAX; d += GLOW_STEP) {

                            float alpha =
                                    0.025F
                                            + (float) (Math.random() * 0.015D)
                                            + (0.125F * flux / 1000F);
                            int argb =
                                    ARGB.color((int) (Mth.clamp(alpha, 0F, 1F) * 255F), GLOW_COLOR);
                            shell(pose, buf, argb, light, d);
                        }
                    });
        }

        ps.popPose();
    }

    private static void shell(
            PoseStack.Pose pose, VertexConsumer buf, int argb, int light, double d) {
        double top = GLOW_TOP + d, bottom = GLOW_BOTTOM - d;

        vertex(pose, buf, argb, light, d, bottom, -d);
        vertex(pose, buf, argb, light, d, top, -d);
        vertex(pose, buf, argb, light, d, top, d);
        vertex(pose, buf, argb, light, d, bottom, d);

        vertex(pose, buf, argb, light, -d, bottom, -d);
        vertex(pose, buf, argb, light, -d, top, -d);
        vertex(pose, buf, argb, light, -d, top, d);
        vertex(pose, buf, argb, light, -d, bottom, d);

        vertex(pose, buf, argb, light, -d, bottom, d);
        vertex(pose, buf, argb, light, -d, top, d);
        vertex(pose, buf, argb, light, d, top, d);
        vertex(pose, buf, argb, light, d, bottom, d);

        vertex(pose, buf, argb, light, -d, bottom, -d);
        vertex(pose, buf, argb, light, -d, top, -d);
        vertex(pose, buf, argb, light, d, top, -d);
        vertex(pose, buf, argb, light, d, bottom, -d);

        vertex(pose, buf, argb, light, -d, top, -d);
        vertex(pose, buf, argb, light, -d, top, d);
        vertex(pose, buf, argb, light, d, top, d);
        vertex(pose, buf, argb, light, d, top, -d);

        vertex(pose, buf, argb, light, -d, bottom, -d);
        vertex(pose, buf, argb, light, -d, bottom, d);
        vertex(pose, buf, argb, light, d, bottom, d);
        vertex(pose, buf, argb, light, d, bottom, -d);
    }

    public static final class State extends BlockEntityRenderState {
        public float controlLevel;
        public int totalFlux;
        public boolean glowing;
    }
}
