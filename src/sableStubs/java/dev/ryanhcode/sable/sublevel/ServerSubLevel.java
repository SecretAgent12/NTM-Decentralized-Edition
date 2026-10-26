// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package dev.ryanhcode.sable.sublevel;

import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.mass.MassData;

/**
 * Compile-time stand-in for Sable's class of the same name (Sable 2.x, 1.21.1): only the methods NTM
 * calls. Not packaged; at run time the class comes from Sable itself.
 */
public class ServerSubLevel {
    public MassData getMassTracker() {
        throw new UnsupportedOperationException("stub");
    }

    public QueuedForceGroup getOrCreateQueuedForceGroup(ForceGroup group) {
        throw new UnsupportedOperationException("stub");
    }
}
