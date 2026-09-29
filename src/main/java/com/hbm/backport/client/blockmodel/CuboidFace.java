// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * 26.x net.minecraft.client.resources.model.cuboid.CuboidFace (the former BlockElementFace):
 * cull direction, tint index, texture reference, optional UVs (null: derived from the element
 * bounds) and a quarter-turn UV rotation.
 */
public record CuboidFace(
        @Nullable Direction cullForDirection,
        int tintIndex,
        String texture,
        @Nullable UVs uvs,
        Quadrant rotation) {

    public static final int NO_TINT = -1;

    public CuboidFace(@Nullable Direction cull, int tintIndex, String texture, @Nullable UVs uvs) {
        this(cull, tintIndex, texture, uvs, Quadrant.R0);
    }

    public boolean isTinted() {
        return tintIndex != NO_TINT;
    }

    /** Face UVs in 0..16 texel space. */
    public record UVs(float minU, float minV, float maxU, float maxV) {

        public float getVertexU(int vertex) {
            return vertex != 0 && vertex != 1 ? maxU : minU;
        }

        public float getVertexV(int vertex) {
            return vertex != 0 && vertex != 3 ? maxV : minV;
        }

        public float[] toArray() {
            return new float[] {minU, minV, maxU, maxV};
        }
    }

    /** Vanilla 1.21.1 BlockElement#uvsByFace: the UVs a face gets when it declares none. */
    public static UVs defaultUvs(org.joml.Vector3fc from, org.joml.Vector3fc to, Direction face) {
        return switch (face) {
            case DOWN -> new UVs(from.x(), 16F - to.z(), to.x(), 16F - from.z());
            case UP -> new UVs(from.x(), from.z(), to.x(), to.z());
            case NORTH -> new UVs(16F - to.x(), 16F - to.y(), 16F - from.x(), 16F - from.y());
            case SOUTH -> new UVs(from.x(), 16F - to.y(), to.x(), 16F - from.y());
            case WEST -> new UVs(from.z(), 16F - to.y(), to.z(), 16F - from.y());
            case EAST -> new UVs(16F - to.z(), 16F - to.y(), 16F - from.z(), 16F - from.y());
        };
    }
}
