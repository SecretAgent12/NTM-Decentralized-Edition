// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 26.x {@code net.minecraft.client.renderer.state.level.CameraRenderState}: a snapshot of the camera
 * for the submit phase. In 1.21.1 it is filled from the live {@link Camera} right before a bridged
 * renderer submits (see {@link #current()}); the vectors are shared, not copied, and must be treated
 * as read-only.
 */
public class CameraRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public Vec3 pos = Vec3.ZERO;
    public boolean initialized;
    public Vec3 entityPos = Vec3.ZERO;
    public Quaternionf orientation = new Quaternionf();
    // backport: unverified: 26.x field names match how NEXT uses them (projectionMatrix, xRot, yRot)
    public Matrix4f projectionMatrix = new Matrix4f();
    public float xRot;
    public float yRot;

    private static final CameraRenderState CURRENT = new CameraRenderState();

    public CameraRenderState() {}

    /** Fills this state from a 1.21.1 camera. */
    public CameraRenderState fill(Camera camera) {
        this.initialized = camera.isInitialized();
        this.pos = camera.getPosition();
        this.blockPos = camera.getBlockPosition();
        this.orientation = camera.rotation();
        this.xRot = camera.getXRot();
        this.yRot = camera.getYRot();
        this.entityPos =
                camera.getEntity() != null
                        ? camera.getEntity().getPosition(
                                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true))
                        : this.pos;
        this.projectionMatrix = RenderSystem.getProjectionMatrix();
        return this;
    }

    /**
     * The main camera as of now (render thread only; the returned instance is reused).
     */
    public static CameraRenderState current() {
        return CURRENT.fill(Minecraft.getInstance().gameRenderer.getMainCamera());
    }

    public static CameraRenderState of(Camera camera) {
        return new CameraRenderState().fill(camera);
    }
}
