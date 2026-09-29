// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

/** 26.x net.minecraft.client.resources.model.geometry.UnbakedGeometry. */
@FunctionalInterface
public interface UnbakedGeometry {
    UnbakedGeometry EMPTY = (slots, baker, state, name) -> QuadCollection.EMPTY;

    QuadCollection bake(TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name);
}
