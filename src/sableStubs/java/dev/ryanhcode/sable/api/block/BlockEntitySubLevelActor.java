// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

/**
 * Compile-time stand-in for Sable's interface of the same name (Sable 2.x, 1.21.1): only the method
 * NTM overrides. Not packaged; at run time the class comes from Sable itself.
 */
public interface BlockEntitySubLevelActor {
    default void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {}
}
