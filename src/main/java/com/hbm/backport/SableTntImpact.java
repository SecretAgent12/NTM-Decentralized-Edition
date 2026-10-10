// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.hbm.blocks.bomb.BlockTNTBase;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-085 — NTM explosive blocks (TNT, semtex, C4, dynamite, fissure bomb) hitting
 * something hard on a physics contraption.
 *
 * <p>Sable makes every vanilla TntBlock explode on a collision faster than 5 m/s
 * (ExplosiveBlockCallback, mixed into TntBlock). NTM's explosives extend TntBlock, so they got the
 * same callback, and it always spawns a vanilla PrimedTnt: a dropped C4 block went off as plain
 * TNT. This callback does the same as Sable's (same trigger speed, a 4 tick fuse, the block is
 * removed) but primes the block's own NTM charge. Hooked up by MixinSableTntImpact, i.e. only
 * with Sable installed.
 */
public final class SableTntImpact implements BlockSubLevelCollisionCallback {

    public static final SableTntImpact INSTANCE = new SableTntImpact();

    /** Sable's ExplosiveBlockCallback.getTriggerVelocity. */
    private static final double TRIGGER_VELOCITY = 5.0;

    /** Sable's ExplosiveBlockCallback fuse. */
    private static final int FUSE = 4;

    private SableTntImpact() {}

    @Override
    public CollisionResult sable$onCollision(
            BlockPos pos, @Nullable BlockPos otherHitBlockPos, Vector3d impactPosition, double impactVelocity) {
        if (impactVelocity * impactVelocity < TRIGGER_VELOCITY * TRIGGER_VELOCITY) return CollisionResult.NONE;

        ServerLevel level = SubLevelPhysicsSystem.getCurrentlySteppingSystem().getLevel();
        BlockState state = level.getBlockState(pos);
        // as Sable's FragileBlockCallback: the contact point may be slightly off; then nothing goes off
        if (!(state.getBlock() instanceof BlockTNTBase tnt)) return new CollisionResult(new Vector3d(), true);

        return tnt.primeOnImpact(level, pos, FUSE)
                ? new CollisionResult(new Vector3d(), true)
                : CollisionResult.NONE;
    }
}
