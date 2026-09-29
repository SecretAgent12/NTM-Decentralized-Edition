// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

/**
 * backport: Flywheel 1.0's AbstractEntityVisual plus CrankShaft's additions: scratch-writing getVisualPosition
 * overloads and the first-person camera-entity culling test.
 */
public abstract class AbstractEntityVisual<T extends Entity> extends dev.engine_room.flywheel.lib.visual.AbstractEntityVisual<T> {
    protected AbstractEntityVisual(VisualizationContext ctx, T entity, float partialTick) {
        super(ctx, entity, partialTick);
    }

    /** Writes render-origin-relative coordinates into caller-owned scratch; no shared mutable result. */
    public Vector3f getVisualPosition(Vector3f destination) {
        Vec3 pos = entity.position();
        Vec3i renderOrigin = renderOrigin();
        return destination.set((float) (pos.x - renderOrigin.getX()), (float) (pos.y - renderOrigin.getY()),
                (float) (pos.z - renderOrigin.getZ()));
    }

    /** Writes interpolated render-origin-relative coordinates into scratch owned by this visual's update task. */
    public Vector3f getVisualPosition(float partialTick, Vector3f destination) {
        Vec3 pos = entity.position();
        Vec3i renderOrigin = renderOrigin();
        return destination.set((float) (Mth.lerp(partialTick, entity.xOld, pos.x) - renderOrigin.getX()),
                (float) (Mth.lerp(partialTick, entity.yOld, pos.y) - renderOrigin.getY()),
                (float) (Mth.lerp(partialTick, entity.zOld, pos.z) - renderOrigin.getZ()));
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        if (isFirstPersonCameraEntity()) {
            return false;
        }
        return super.isVisible(frustum);
    }

    /**
     * Vanilla's first-person camera-entity suppression; visualized entities bypass that test, so a ridden/spectated
     * visual must re-apply it.
     */
    protected boolean isFirstPersonCameraEntity() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        return camera.getEntity() == entity && !camera.isDetached()
                && !(entity instanceof LivingEntity living && living.isSleeping());
    }
}
