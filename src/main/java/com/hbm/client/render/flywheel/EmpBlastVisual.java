// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import com.hbm.backport.client.flywheel.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import com.hbm.backport.client.flywheel.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import com.hbm.lib.crankshaft.OverlayTexture;
import com.hbm.backport.ARGB;
import net.minecraft.client.renderer.LightTexture;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class EmpBlastVisual extends HbmDynamicEntityVisual<EntityEMPBlast> {
    private static final Model MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(
                            ResourceManager.emp_ring,
                            ResourceManager.emp_ring.partId("Circle_Circle.001")),
                    SimpleMaterial.builder()
                            .texture(ResourceManager.emp_ring_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .useOverlay(false)
                            .useLight(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .backfaceCulling(false)
                            .build());
    private final Vector3f interpolatedPosition = new Vector3f();
    private final TransformedInstance ring;
    private final Matrix4f pose = new Matrix4f();
    private float lastX = Float.NaN, lastY, lastZ, lastScale;

    public EmpBlastVisual(VisualizationContext ctx, EntityEMPBlast entity, float partialTick) {
        super(ctx, entity, partialTick);
        ring = instancerProvider().instancer(InstanceTypes.TRANSFORMED, MODEL).createInstance();
        ring.overlay(OverlayTexture.NO_OVERLAY);
        ring.light(LightTexture.FULL_BRIGHT);
        ring.colorArgb(ARGB.color(255, 255, 255, 255));
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        var sphere = MODEL.boundingSphere();
        float scale = entity.scale;
        float x = sphere.x() * scale, z = sphere.z() * scale;
        float reach =
                (float) Math.sqrt(x * x + sphere.y() * sphere.y() + z * z)
                        + sphere.w() * Math.max(Math.abs(scale), 1F);
        return sphereVisible(frustum, 0F, reach);
    }

    @Override
    protected void frame(DynamicVisual.Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float scale = entity.scale;
        if (visualPos.x == lastX
                && visualPos.y == lastY
                && visualPos.z == lastZ
                && scale == lastScale) return;
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastScale = scale;
        ring.setTransform(
                pose.translation(visualPos.x, visualPos.y, visualPos.z).scale(scale, 1F, scale));
        ring.setChanged();
    }

    @Override
    protected void _delete() {
        ring.delete();
    }
}
