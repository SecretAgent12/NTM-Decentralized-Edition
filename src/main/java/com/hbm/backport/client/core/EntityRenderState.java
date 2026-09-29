// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.entity.state.EntityRenderState}. */
public class EntityRenderState {
    public static final int NO_OUTLINE = 0;

    public EntityType<?> entityType = EntityType.PIG;
    public double x;
    public double y;
    public double z;
    public float ageInTicks;
    public float boundingBoxWidth;
    public float boundingBoxHeight;
    public float eyeHeight;
    public double distanceToCameraSq;
    public boolean isInvisible;
    public boolean isDiscrete;
    public boolean displayFireAnimation;
    public int lightCoords = 0xF000F0;
    public int outlineColor = NO_OUTLINE;
    public @Nullable Vec3 passengerOffset;
    public @Nullable Component nameTag;
    public @Nullable Vec3 nameTagAttachment;
    public @Nullable List<LeashState> leashStates;
    public float shadowRadius;
    public final List<ShadowPiece> shadowPieces = new ArrayList<>();
    // backport: unverified: partial tick kept on the state (used by one tree renderer)
    public float partialTick;

    /**
     * The base 26.x EntityRenderer extraction for a 1.21.1 entity (outside a bridged renderer, e.g.
     * armor/player hooks). Name tag fields are left empty.
     */
    public static void extractEntity(
            net.minecraft.world.entity.Entity entity,
            EntityRenderState state,
            float partialTicks,
            int lightCoords) {
        state.entityType = entity.getType();
        state.x = net.minecraft.util.Mth.lerp(partialTicks, entity.xOld, entity.getX());
        state.y = net.minecraft.util.Mth.lerp(partialTicks, entity.yOld, entity.getY());
        state.z = net.minecraft.util.Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        state.partialTick = partialTicks;
        state.isInvisible = entity.isInvisible();
        state.ageInTicks = entity.tickCount + partialTicks;
        state.boundingBoxWidth = entity.getBbWidth();
        state.boundingBoxHeight = entity.getBbHeight();
        state.eyeHeight = entity.getEyeHeight();
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        state.distanceToCameraSq = mc.getEntityRenderDispatcher().distanceToSqr(entity);
        state.isDiscrete = entity.isDiscrete();
        state.displayFireAnimation = entity.displayFireAnimation();
        state.lightCoords = lightCoords;
        state.outlineColor =
                mc.shouldEntityAppearGlowing(entity) ? entity.getTeamColor() | 0xFF000000 : NO_OUTLINE;
    }

    public boolean appearsGlowing() {
        return outlineColor != NO_OUTLINE;
    }

    public record ShadowPiece(
            float relativeX, float relativeY, float relativeZ, VoxelShape shapeBelow, float alpha) {}

    public static class LeashState {
        public Vec3 offset = Vec3.ZERO;
        public Vec3 start = Vec3.ZERO;
        public Vec3 end = Vec3.ZERO;
        public int startBlockLight = 0;
        public int endBlockLight = 0;
        public int startSkyLight = 15;
        public int endSkyLight = 15;
        public boolean slack = true;
    }
}
