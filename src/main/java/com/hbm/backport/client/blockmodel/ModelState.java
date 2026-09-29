// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.mojang.math.Transformation;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * 26.x net.minecraft.client.resources.model.ModelState: a block-centred transformation plus the
 * per-face UV-lock transforms. {@link #vanilla()} gives the 1.21.1 ModelState FaceBakery takes.
 */
public interface ModelState {
    Matrix4fc NO_TRANSFORM = new Matrix4f();

    default Transformation transformation() {
        return Transformation.identity();
    }

    default Matrix4fc faceTransformation(Direction direction) {
        return NO_TRANSFORM;
    }

    default Matrix4fc inverseFaceTransformation(Direction direction) {
        return NO_TRANSFORM;
    }

    default boolean isUvLocked() {
        return false;
    }

    default net.minecraft.client.resources.model.ModelState vanilla() {
        ModelState self = this;
        return new net.minecraft.client.resources.model.ModelState() {
            @Override
            public Transformation getRotation() {
                return self.transformation();
            }

            @Override
            public boolean isUvLocked() {
                return self.isUvLocked();
            }
        };
    }

    static ModelState of(net.minecraft.client.resources.model.ModelState vanilla) {
        return new Vanilla(vanilla);
    }

    record Vanilla(net.minecraft.client.resources.model.ModelState state) implements ModelState {
        @Override
        public Transformation transformation() {
            return state.getRotation();
        }

        @Override
        public boolean isUvLocked() {
            return state.isUvLocked();
        }

        @Override
        public net.minecraft.client.resources.model.ModelState vanilla() {
            return state;
        }
    }
}
