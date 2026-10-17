package dev.ryanhcode.sable.api.physics.callback;

import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Compile-time stand-in for Sable's interface of the same name (Sable 2.x, 1.21.1): only the
 * signatures NTM uses. Not packaged; at run time the class comes from Sable itself.
 */
public interface BlockSubLevelCollisionCallback {

    CollisionResult sable$onCollision(
            BlockPos hitBlockPos, BlockPos otherHitBlockPos, Vector3d impactPosition, double impactVelocity);

    record CollisionResult(Vector3dc tangentMotion, boolean removeCollision) {
        public static final CollisionResult NONE = new CollisionResult(new Vector3d(), false);
    }
}
