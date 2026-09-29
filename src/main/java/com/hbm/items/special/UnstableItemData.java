// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.backport.IUnstableDeadline;
import net.minecraft.core.component.PatchedDataComponentMap;
import com.hbm.backport.ItemStackTemplate;

public final class UnstableItemData {
    public static final long UNRESOLVED = Long.MIN_VALUE;

    private UnstableItemData() {}

    public static long cached(PatchedDataComponentMap components) {
        return ((IUnstableDeadline) (Object) components).hbm$unstableDeadline();
    }

    public static void cache(PatchedDataComponentMap components, long deadline) {
        ((IUnstableDeadline) (Object) components).hbm$setUnstableDeadline(deadline);
    }

    public static long cached(ItemStackTemplate template) {
        return ((IUnstableDeadline) (Object) template).hbm$unstableDeadline();
    }

    public static void cache(ItemStackTemplate template, long deadline) {
        ((IUnstableDeadline) (Object) template).hbm$setUnstableDeadline(deadline);
    }
}
