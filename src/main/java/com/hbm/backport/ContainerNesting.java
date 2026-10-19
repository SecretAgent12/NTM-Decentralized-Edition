// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * backport-fix: BF-102 the "no containers inside containers" checks of NTM: NEXT 1.0.3 reject every stack that
 * has a {@code minecraft:container} component. In 1.21.1 vanilla gives that component, empty, to the items of
 * chests, trapped chests, barrels, hoppers, dispensers, droppers, furnaces and the like, so crates, mass storage,
 * pneumatic storage, toolboxes and cargo shells refused even empty chests and hoppers. An empty chest item can't
 * be filled while it lies in a slot, so it can't nest an inventory; only a stack that already holds items can.
 * Bundles stay refused as before: they can be filled in place by clicking items onto them.
 */
public final class ContainerNesting {
    private ContainerNesting() {}

    /** Whether the stack carries a non-empty {@code minecraft:container} (an item that holds items). */
    public static boolean holdsItems(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItems()
                .iterator()
                .hasNext();
    }
}
