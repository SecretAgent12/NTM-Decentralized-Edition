// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;

/**
 * 26.x {@code net.minecraft.client.renderer.entity.MobRenderer<T, S, M>}. backport: leashes are not
 * drawn for these mobs (1.21.1 draws them in its own MobRenderer, which this does not extend).
 */
public abstract class MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends LivingEntityRenderer<T, S, M> {

    protected MobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    /** 1.21.1 MobRenderer.shouldShowName. */
    @Override
    protected boolean shouldShowName(T entity) {
        return super.shouldShowName(entity)
                && (entity.shouldShowName()
                        || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
    }
}
