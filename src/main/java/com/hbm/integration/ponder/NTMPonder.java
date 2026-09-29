// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ponder;

import net.createmod.ponder.foundation.PonderIndex;

/**
 * backport: Ponder guides (not in ntm-next). Only reached behind {@code ModList.isLoaded("ponder")}
 * (NuclearTechNeoForgeClient), so no Ponder class loads without Ponder.
 */
public final class NTMPonder {
    private NTMPonder() {}

    public static void register() {
        PonderIndex.addPlugin(new NTMPonderPlugin());
    }
}
