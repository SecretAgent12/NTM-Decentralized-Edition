// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * 26.x {@code ExtraCodecs.VECTOR3F} / {@code MATRIX4F} are typed on the read-only JOML interfaces
 * (Vector3fc / Matrix4fc); 1.21.1's are on the mutable classes.
 */
public final class JomlCodecs {
    private JomlCodecs() {}

    public static final Codec<Vector3fc> VECTOR3F =
            ExtraCodecs.VECTOR3F.xmap(v -> v, v -> v instanceof Vector3f f ? f : new Vector3f(v));

    public static final Codec<Matrix4fc> MATRIX4F =
            ExtraCodecs.MATRIX4F.xmap(m -> m, m -> m instanceof Matrix4f f ? f : new Matrix4f(m));
}
