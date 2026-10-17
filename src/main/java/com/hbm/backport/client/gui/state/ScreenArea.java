// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.gui.state;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.state.gui.ScreenArea}. */
public interface ScreenArea {
    @Nullable ScreenRectangle bounds();
}
