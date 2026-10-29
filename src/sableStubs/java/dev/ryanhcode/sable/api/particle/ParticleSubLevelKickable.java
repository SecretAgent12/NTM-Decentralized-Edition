// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package dev.ryanhcode.sable.api.particle;

/**
 * Compile-time stand-in for Sable's interface of the same name (Sable 2.x, 1.21.1): the methods a
 * particle answers. Not packaged; at run time the class comes from Sable itself.
 */
public interface ParticleSubLevelKickable {
    default boolean sable$shouldCareAboutIntersectingSubLevels() {
        return true;
    }

    boolean sable$shouldKickFromTracking();

    boolean sable$shouldCollideWithTrackingSubLevel();
}
