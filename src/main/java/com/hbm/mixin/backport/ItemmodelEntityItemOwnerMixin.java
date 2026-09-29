// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.client.itemmodel.ItemOwner;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * backport: 26.x entities are {@code ItemOwner}s (the holder passed to item models). 1.21.1 Entity
 * already has level(), position() and getVisualRotationYInDegrees() with the same signatures; this
 * only adds the interface so {@link ItemOwner#of} casts hold and owner identity checks work.
 */
@Mixin(Entity.class)
public abstract class ItemmodelEntityItemOwnerMixin implements ItemOwner {}
