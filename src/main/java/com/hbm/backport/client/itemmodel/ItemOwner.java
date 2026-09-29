// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.world.entity.ItemOwner}: whoever holds/displays an item being rendered.
 * In 26.x entities implement it; here {@code com.hbm.mixin.backport.ItemmodelEntityItemOwnerMixin}
 * adds it to {@link Entity} (whose level()/position()/getVisualRotationYInDegrees() already match),
 * so owner identity checks ({@code owner == player}, {@code instanceof LivingEntity}) keep working.
 */
public interface ItemOwner {
    Level level();

    Vec3 position();

    float getVisualRotationYInDegrees();

    default @Nullable LivingEntity asLivingEntity() {
        return this instanceof LivingEntity living ? living : null;
    }

    /** backport: 1.21.1 entities are not statically ItemOwners; the mixin makes this cast valid. */
    static @Nullable ItemOwner of(@Nullable Entity entity) {
        return (ItemOwner) entity;
    }
}
