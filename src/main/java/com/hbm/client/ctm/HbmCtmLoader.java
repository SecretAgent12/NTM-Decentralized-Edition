// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.ctm;

/**
 * backport: the 26.x class registers a custom Continuity CTM method "hbm_ctm" (47-tile CTM whose
 * {@code connectBlocks} also joins listed neighbours), built on Continuity internals
 * ({@code me.pepperbell.continuity.client.properties.OrientedConnectingCtmProperties},
 * {@code BaseCachingPredicates}, {@code ConnectionPredicate}, ...). Continuity is not on the 1.21.1
 * compile classpath and its 1.21.1 NeoForge build (3.0.0+1.21.neoforge) cannot be checked here, so
 * the loader is a no-op for now: with Continuity installed the ctm_continuity pack still applies its
 * {@code method=ctm} files; the six {@code method=hbm_ctm} files are skipped by Continuity (those
 * blocks show their plain texture). The 26.x source is at aca82fe0 for re-enabling once the jar is
 * on the classpath.
 */
public final class HbmCtmLoader {

    public static final String METHOD = "hbm_ctm";

    private HbmCtmLoader() {}

    public static void register() {}
}
