// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-054 — how high a dropped item rests, the 26.x way: its model's lowest point
 * (GROUND display, from the 26.x item definition) 1/16 above the ground. 1.21.1 uses a fixed {@code
 * 0.25 * ground scale} instead, which sinks the 3D models NEXT's items are made of (they're scaled
 * down a lot on the ground, so that lift is tiny). Used by MixinItemEntityRenderer. Worked out once
 * per item; cleared when the item models are baked again. Render thread only.
 */
public final class GroundLift {

    private static final Map<Item, Float> LIFTS = new HashMap<>();

    private GroundLift() {}

    static void clear() {
        LIFTS.clear();
    }

    public static float of(ItemStack stack, @Nullable Level level, int seed) {
        Float cached = LIFTS.get(stack.getItem());
        if (cached != null) return cached;
        ItemStackRenderState state = new ItemStackRenderState();
        ItemModelResolver.get()
                .updateForTopItem(state, stack, ItemDisplayContext.GROUND, level, null, seed);
        AABB box = state.getModelBoundingBox();
        state.clear();
        float lift = -(float) box.minY + 0.0625F;
        LIFTS.put(stack.getItem(), lift);
        // TEMP DEBUG BF-054
        com.hbm.NuclearTech.LOGGER.info("[BF-054] {} box={} lift={}", stack.getItem(), box, lift);
        return lift;
    }
}
