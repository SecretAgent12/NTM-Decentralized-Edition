// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.inventory.recipes;

import com.hbm.items.machine.ItemStamp;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * backport-fix: BF-128 (GitHub #47) press stamp types that datapacks and KubeJS can extend.
 *
 * <p>A stamp type is a name; a press recipe asks for one ({@code "stamp": "plate"}), and an item
 * presses with every type it has:
 *
 * <ul>
 *   <li>NTM's own stamps ({@link ItemStamp}) have their built-in type, as in 1.7.10;
 *   <li>any item in the item tag {@code hbm:stamps/<type>} has that type too, so other mods' items
 *       can work as stamps (an AE2 inscriber press in {@code hbm:stamps/circuit}) and new types need
 *       nothing but a tag and recipes asking for it ({@code hbm:stamps/ae2_logic}).
 * </ul>
 *
 * <p>Items with a type go into the press's stamp slot and stay out of its input slot. Only NTM's
 * own stamps wear out; an item without durability presses forever.
 */
public final class StampTypes {

    private static final String NAMESPACE = Library.id("stamps").getNamespace();
    private static final String PREFIX = "stamps/";

    private StampTypes() {}

    /** The name of a built-in type, as press recipes write it. */
    public static String name(ItemStamp.StampType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    /** The item tag that gives items the stamp type {@code type}. */
    public static TagKey<Item> tag(String type) {
        return TagKey.create(Registries.ITEM, Library.id(PREFIX + type));
    }

    /** Every stamp type of {@code stack}: its own (NTM stamps) first, then its tags. */
    public static Set<String> typesOf(ItemStack stack) {
        if (stack.isEmpty()) return Set.of();
        Set<String> types = new LinkedHashSet<>(0x4);
        if (stack.getItem() instanceof ItemStamp stamp && stamp.getStampType(stack) != null)
            types.add(name(stamp.getStampType(stack)));
        stack.getTags()
                .forEach(
                        tag -> {
                            ResourceLocation id = tag.location();
                            if (!NAMESPACE.equals(id.getNamespace())
                                    || !id.getPath().startsWith(PREFIX)
                                    || id.getPath().length() <= PREFIX.length()) return;
                            types.add(id.getPath().substring(PREFIX.length()));
                        });
        return types;
    }

    /** Whether {@code stack} goes into a press's stamp slot. */
    public static boolean isStamp(ItemStack stack) {
        return !typesOf(stack).isEmpty();
    }

    /** The items that press with stamp type {@code type}, NTM's stamps first (for recipe viewers). */
    public static List<Item> stampsOf(String type) {
        List<Item> items = new ArrayList<>();
        for (ItemStamp.StampType builtIn : ItemStamp.StampType.values()) {
            if (name(builtIn).equals(type)) items.addAll(ItemStamp.stampsOf(builtIn));
        }
        BuiltInRegistries.ITEM
                .getTag(tag(type))
                .ifPresent(
                        set -> {
                            for (Holder<Item> holder : set) {
                                if (!items.contains(holder.value())) items.add(holder.value());
                            }
                        });
        return items;
    }
}
