// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.recipe;

/** 26.x RecipeBookCategory (a registered marker object); 1.21.1 has no counterpart. */
public final class RecipeBookCategory {

    public static final RecipeBookCategory UNUSED = new RecipeBookCategory();

    public RecipeBookCategory() {}
}
