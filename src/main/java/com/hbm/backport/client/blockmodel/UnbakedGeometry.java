// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.blockmodel;

/** 26.x net.minecraft.client.resources.model.geometry.UnbakedGeometry. */
@FunctionalInterface
public interface UnbakedGeometry {
    UnbakedGeometry EMPTY = (slots, baker, state, name) -> QuadCollection.EMPTY;

    QuadCollection bake(TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name);
}
