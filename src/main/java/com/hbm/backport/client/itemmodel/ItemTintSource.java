// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.color.item.ItemTintSource}: one tint layer of an item model. */
public interface ItemTintSource {
    int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner);

    MapCodec<? extends ItemTintSource> type();
}
