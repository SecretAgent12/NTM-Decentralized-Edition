// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.BakedQuad;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * 26.x {@code net.minecraft.client.renderer.item.CuboidItemModelWrapper}: only its extents helper is
 * used by the tree (the wrapper itself is {@link BlockModelWrapper} here).
 */
public final class CuboidItemModelWrapper {
    private CuboidItemModelWrapper() {}

    public static Vector3fc[] computeExtents(List<BakedQuad> quads) {
        Set<Vector3fc> points = new HashSet<>();
        for (BakedQuad quad : quads)
            for (int v = 0; v < 4; v++) points.add(new Vector3f(quad.position(v)));
        return points.toArray(new Vector3fc[0]);
    }
}
