package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;

/**
 * Compile-time stand-in for Sable's interface of the same name (Sable 2.x, 1.21.1): only the
 * signature the optional Sable mixin overrides. Not packaged; at run time the class comes from
 * Sable itself (Sable makes vanilla TntBlock implement it).
 */
public interface BlockWithSubLevelCollisionCallback {
    BlockSubLevelCollisionCallback sable$getCallback();
}
