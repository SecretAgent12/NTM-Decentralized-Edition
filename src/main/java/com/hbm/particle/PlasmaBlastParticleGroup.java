// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.client.render.ParticleRenderTypes;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import com.hbm.backport.client.particle.ParticleGroup;
import com.hbm.backport.client.core.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.particle.ParticleGroupRenderState;
import com.hbm.backport.ARGB;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;

public class PlasmaBlastParticleGroup extends ParticleGroup<ParticlePlasmaBlast> {

    public PlasmaBlastParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Instance> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.getPosition();

        for (ParticlePlasmaBlast blast : this.particles) {
            out.add(
                    new Instance(
                            blast.interpX(partialTicks) - cam.x,
                            blast.interpY(partialTicks) - cam.y,
                            blast.interpZ(partialTicks) - cam.z,
                            blast.yaw,
                            blast.pitch,
                            blast.red,
                            blast.green,
                            blast.blue,
                            blast.alpha(partialTicks),
                            blast.renderScale(partialTicks)));
        }

        return new State(out);
    }

    private record Instance(
            double relX,
            double relY,
            double relZ,
            float yaw,
            float pitch,
            float r,
            float g,
            float b,
            float alpha,
            float scale) {}

    private record State(List<Instance> instances) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack pose = new PoseStack();

            for (Instance in : instances) {
                pose.pushPose();
                pose.translate(in.relX, in.relY, in.relZ);
                pose.mulPose(Axis.YP.rotationDegrees(in.yaw));
                pose.mulPose(Axis.XP.rotationDegrees(in.pitch));

                float s = in.scale;
                int color = ARGB.colorFromFloat(in.alpha, in.r, in.g, in.b);
                collector.submitCustomGeometry(
                        pose,
                        ParticleRenderTypes.flashNoFog(ResourceManager.shockwave_tex),
                        (p, tess) -> {
                            Vertices.emit(
                                    tess,
                                    p,
                                    -s,
                                    0,
                                    -s,
                                    color,
                                    1,
                                    1,
                                    LightTexture.pack(15, 0),
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    tess,
                                    p,
                                    -s,
                                    0,
                                    s,
                                    color,
                                    1,
                                    0,
                                    LightTexture.pack(15, 0),
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    tess,
                                    p,
                                    s,
                                    0,
                                    s,
                                    color,
                                    0,
                                    0,
                                    LightTexture.pack(15, 0),
                                    0F,
                                    1F,
                                    0F);
                            Vertices.emit(
                                    tess,
                                    p,
                                    s,
                                    0,
                                    -s,
                                    color,
                                    0,
                                    1,
                                    LightTexture.pack(15, 0),
                                    0F,
                                    1F,
                                    0F);
                        });
                pose.popPose();
            }
        }
    }
}
