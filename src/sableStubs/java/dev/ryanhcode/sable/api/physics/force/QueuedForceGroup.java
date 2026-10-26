// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package dev.ryanhcode.sable.api.physics.force;

import org.joml.Vector3dc;

/**
 * Compile-time stand-in for Sable's class of the same name (Sable 2.x, 1.21.1): only the method NTM
 * calls. Not packaged; at run time the class comes from Sable itself.
 */
public class QueuedForceGroup {
    public void applyAndRecordPointForce(Vector3dc point, Vector3dc force) {
        throw new UnsupportedOperationException("stub");
    }
}
