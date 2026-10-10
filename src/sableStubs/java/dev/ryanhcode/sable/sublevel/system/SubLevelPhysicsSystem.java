// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package dev.ryanhcode.sable.sublevel.system;

import net.minecraft.server.level.ServerLevel;

/**
 * Compile-time stand-in for Sable's class of the same name (Sable 2.x, 1.21.1): only the two
 * methods NTM calls. Not packaged; at run time the class comes from Sable itself.
 */
public class SubLevelPhysicsSystem {

    public static SubLevelPhysicsSystem getCurrentlySteppingSystem() {
        throw new UnsupportedOperationException("stub");
    }

    public ServerLevel getLevel() {
        throw new UnsupportedOperationException("stub");
    }
}
