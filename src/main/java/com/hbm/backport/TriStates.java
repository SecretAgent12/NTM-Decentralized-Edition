// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import net.neoforged.neoforge.common.util.TriState;

/** 26.x TriState members that NeoForge 21.1's {@link TriState} lacks (call sites use these instead). */
public final class TriStates {

    private TriStates() {}

    /** 26.x {@code TriState.toBoolean(fallback)}: TRUE/FALSE as-is, DEFAULT as {@code fallback}. */
    public static boolean toBoolean(TriState state, boolean fallback) {
        return state.isDefault() ? fallback : state.isTrue();
    }
}
