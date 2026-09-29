// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.tool;

import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 26.x {@code Item.Properties} builder methods that 1.21.1 lacks, as statics
 * ({@code props.enchantable(n)} -> {@code ItemProps26.enchantable(props, n)}, rewritten by the
 * backport). They set the shim components; MixinItemToolShims makes them effective.
 * Items whose class supplies these natively (1.21.1 ArmorItem / TieredItem) never reach the
 * mixin's component path, since those classes override the methods it hooks.
 */
public final class ItemProps26 {

    private ItemProps26() {}

    /**
     * 26.x #minecraft:sword_instantly_mines (bamboo, bamboo sapling). 1.21.1 has no such tag: the
     * same blocks are made instant by their getDestroyProgress for SWORD_DIG items; a speed
     * override to Float.MAX_VALUE on them has the same effect for any item with the rule.
     */
    public static final List<Block> SWORD_INSTANTLY_MINES = List.of(Blocks.BAMBOO, Blocks.BAMBOO_SAPLING);

    public static Item.Properties enchantable(Item.Properties properties, int enchantmentValue) {
        return properties.component(ToolRegistry.ENCHANTABLE.get(), new Enchantable(enchantmentValue));
    }

    public static Item.Properties repairable(Item.Properties properties, TagKey<Item> repairItems) {
        return properties.component(
                ToolRegistry.REPAIRABLE.get(),
                new Repairable(BuiltInRegistries.ITEM.getOrCreateTag(repairItems)));
    }

    public static Item.Properties repairable(Item.Properties properties, ItemLike repairItem) {
        return properties.component(
                ToolRegistry.REPAIRABLE.get(),
                new Repairable(HolderSet.direct(repairItem.asItem().builtInRegistryHolder())));
    }

    public static Item.Properties repairable(Item.Properties properties, HolderSet<Item> repairItems) {
        return properties.component(ToolRegistry.REPAIRABLE.get(), new Repairable(repairItems));
    }
}
