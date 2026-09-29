// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.CameraRenderState;
import org.joml.Quaternionf;

public class RenderBeam extends EntityRenderer<EntityBulletBeamBase, RenderBeam.State>
        implements ConcurrentRenderStateExtraction {

    private static final Map<BulletConfig, BeamDrawer> RENDERERS = new HashMap<>();

    public RenderBeam(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void setRenderer(BulletConfig config, BeamDrawer drawer) {
        RENDERERS.put(config, drawer);
    }

    @Override
    protected boolean affectedByCulling(EntityBulletBeamBase entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityBulletBeamBase beam, State state, float partialTicks) {
        super.extractRenderState(beam, state, partialTicks);
        if (beam.config == null) beam.config = beam.getBulletConfig();
        state.config = beam.config;
        state.beamLength = beam.getBeamLength();
        state.yaw = beam.getExactYaw();
        state.pitch = beam.getExactPitch();
        state.age = beam.tickCount;
        state.partialTicks = partialTicks;
        state.entityId = beam.getId();
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.config == null) return;

        state.cameraOrientation = camera.orientation;
        pose.pushPose();

        if (state.config.renderRotations) {
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(state.pitch + 180));
        }

        BeamDrawer drawer = RENDERERS.get(state.config);
        if (drawer != null) drawer.draw(state, pose, collector);

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public interface BeamDrawer {
        void draw(State state, PoseStack pose, SubmitNodeCollector collector);
    }

    public static class State extends EntityRenderState {
        public BulletConfig config;
        public double beamLength;
        public float yaw;
        public float pitch;
        public int age;
        public float partialTicks;
        public int entityId;
        public Quaternionf cameraOrientation;
    }
}
