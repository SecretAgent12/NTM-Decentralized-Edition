// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;

/**
 * Backport of 26.x's InsideBlockEffectApplier.
 *
 * 26.x queues the effects of every block an entity stands in and applies them
 * once per tick in a fixed order. 1.21.1 has no queue: entityInside is called per
 * block and acts immediately, so this applier does the same. The effects are the
 * 1.21.1 calls that 26.x's own effect types stand for.
 */
public interface InsideBlockEffectApplier {
    void apply(InsideBlockEffectType type);

    void runBefore(InsideBlockEffectType type, Consumer<Entity> effect);

    void runAfter(InsideBlockEffectType type, Consumer<Entity> effect);

    static InsideBlockEffectApplier immediate(Entity entity) {
        return new InsideBlockEffectApplier() {
            @Override
            public void apply(InsideBlockEffectType type) {
                switch (type) {
                    case EXTINGUISH -> entity.clearFire();
                    case CLEAR_FREEZE -> entity.setTicksFrozen(0);
                    case LAVA_IGNITE -> entity.lavaHurt(); // 1.21.1: ignite + lava damage in one call
                    case FIRE_IGNITE, FREEZE -> {}
                }
            }

            @Override
            public void runBefore(InsideBlockEffectType type, Consumer<Entity> effect) {
                effect.accept(entity);
            }

            @Override
            public void runAfter(InsideBlockEffectType type, Consumer<Entity> effect) {
                effect.accept(entity);
            }
        };
    }
}
