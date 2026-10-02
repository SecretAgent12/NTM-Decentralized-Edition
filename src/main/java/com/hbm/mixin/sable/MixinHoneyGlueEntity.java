// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.sable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * backport-fix: BF-030 Create: Aeronautics' honey glue is an entity with a hitbox the size of the
 * whole build, and it never takes damage (hurt() returns false). A vanilla explosion still traces
 * its seen-percentage rays through that hitbox first: tens of thousands of clips per blast next to a
 * glued build (NTM interceptors, TNT, creepers alike). The glue now ignores explosions, so they skip
 * it, exactly as they already skip its (refused) damage.
 *
 * <p>Applied only when Aeronautics ("simulated") is installed (SableMixinPlugin); no compile-time
 * dependency on it.
 */
@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity")
public abstract class MixinHoneyGlueEntity extends Entity {

    private MixinHoneyGlueEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }
}
