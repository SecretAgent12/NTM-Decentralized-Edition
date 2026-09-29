// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Transformation;
import java.util.EnumMap;
import java.util.Map;
import org.joml.Matrix4f;

/**
 * 26.x net.minecraft.client.resources.model.BlockModelRotation: a model state rotating by an
 * octahedral group element (26.x blockstate rotations are group elements, see
 * com.hbm.backport.Octahedral for the BLOCK_ROT_* names).
 */
public final class BlockModelRotation implements ModelState {

    private static final Map<OctahedralGroup, BlockModelRotation> BY_GROUP = new EnumMap<>(OctahedralGroup.class);

    static {
        for (OctahedralGroup group : OctahedralGroup.values()) BY_GROUP.put(group, new BlockModelRotation(group));
    }

    public static final BlockModelRotation IDENTITY = get(OctahedralGroup.IDENTITY);

    private final OctahedralGroup group;
    private final Transformation transformation;

    private BlockModelRotation(OctahedralGroup group) {
        this.group = group;
        this.transformation = group == OctahedralGroup.IDENTITY
                ? Transformation.identity()
                : new Transformation(new Matrix4f(com.hbm.backport.Octahedral.matrix(group)) /* 1.21.1 transformation() is wrong, see Octahedral */);
    }

    public static BlockModelRotation get(OctahedralGroup group) {
        return BY_GROUP.get(group);
    }

    public OctahedralGroup group() {
        return group;
    }

    @Override
    public Transformation transformation() {
        return transformation;
    }
}
