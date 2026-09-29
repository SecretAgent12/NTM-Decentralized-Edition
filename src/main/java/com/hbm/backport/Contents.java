// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * 26.x item-holding components store ItemStackTemplates (container slots as
 * Optional<ItemStackTemplate>, bundles and charged projectiles as lists of templates);
 * 1.21.1's store ItemStacks. Conversions both ways for code written against 26.x.
 */
public final class Contents {

    private Contents() {}

    private static Optional<ItemStackTemplate> slot(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : Optional.of(ItemStackTemplate.fromNonEmptyStack(stack));
    }

    private static List<ItemStackTemplate> templates(Iterable<ItemStack> stacks) {
        List<ItemStackTemplate> out = new ArrayList<>();
        for (ItemStack s : stacks) if (!s.isEmpty()) out.add(ItemStackTemplate.fromNonEmptyStack(s));
        return out;
    }

    private static List<ItemStack> stacks(List<ItemStackTemplate> templates) {
        return templates.stream().map(ItemStackTemplate::create).toList();
    }

    /** 26.x ItemContainerContents.items. */
    public static List<Optional<ItemStackTemplate>> items(ItemContainerContents contents) {
        List<Optional<ItemStackTemplate>> out = new ArrayList<>(contents.getSlots());
        for (int i = 0; i < contents.getSlots(); i++) out.add(slot(contents.getStackInSlot(i)));
        return out;
    }

    /** 26.x new ItemContainerContents(List<Optional<ItemStackTemplate>>). */
    public static ItemContainerContents container(List<Optional<ItemStackTemplate>> slots) {
        return ItemContainerContents.fromItems(
                slots.stream().map(o -> o.map(ItemStackTemplate::create).orElse(ItemStack.EMPTY)).toList());
    }

    /** 26.x BundleContents.items(). */
    public static List<ItemStackTemplate> items(BundleContents bundle) {
        return templates(bundle.items());
    }

    /** 26.x new BundleContents(items, selected): 1.21.1 bundles have no selected slot. */
    public static BundleContents bundle(List<ItemStackTemplate> items, int selected) {
        return new BundleContents(stacks(items));
    }

    /** 26.x ChargedProjectiles.items(). */
    public static List<ItemStackTemplate> items(ChargedProjectiles projectiles) {
        return templates(projectiles.getItems());
    }

    /** 26.x new ChargedProjectiles(items). */
    public static ChargedProjectiles projectiles(List<ItemStackTemplate> items) {
        return ChargedProjectiles.of(stacks(items));
    }
}
