// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.lib.Library;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

public final class PageIds {

    private PageIds() {}

    public static ResourceLocation page(String path) {
        return Library.id(path);
    }

    public static ResourceLocation derived(ResourceLocation page, String... segments) {
        return page.withSuffix("/" + String.join("/", segments));
    }

    public static String segment(ResourceLocation id) {
        return id.getNamespace() + "/" + id.getPath();
    }

    public static String segment(Enum<?> constant) {
        return constant.name().toLowerCase(Locale.ROOT);
    }
}
