// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty}. */
public interface RangeSelectItemModelProperty {
    float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed);

    MapCodec<? extends RangeSelectItemModelProperty> type();
}
