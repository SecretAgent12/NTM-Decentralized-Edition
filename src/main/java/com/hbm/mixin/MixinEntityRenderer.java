// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.CullableRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * backport: 26.x affectedByCulling lives on the vanilla EntityRenderer; 1.21.1's has none (vanilla
 * renderers are always culled by their bounding box). The 26.x-shaped renderers of the tree extend
 * the core shim {@link com.hbm.backport.client.core.EntityRenderer}, which carries the method, so
 * this mixin exposes it there (MixinLevelExtractor's unculled-entity check). The 26.x extraction
 * hook (vanished flag, motion) moved to MixinEntityRenderState / MixinHumanoidRenderState, the
 * extraction helpers every 1.21.1 path (bridged renderers and HumanoidRenderState.of) runs.
 */
@Mixin(com.hbm.backport.client.core.EntityRenderer.class)
public abstract class MixinEntityRenderer implements CullableRenderer {

    @Shadow(remap = false)
    protected abstract boolean affectedByCulling(Entity entity);

    @Override
    public boolean hbm$affectedByCulling(Entity entity) {
        return affectedByCulling(entity);
    }
}
