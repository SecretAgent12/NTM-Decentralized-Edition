// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.tool;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

/**
 * 26.x {@code new HoeItem(ToolMaterial, attackDamage, attackSpeed, properties)} on 1.21.1's
 * {@code HoeItem(Tier, properties)}: the attack attributes 26.x builds in
 * ToolMaterial.applyToolProperties are 1.21.1's DiggerItem.createAttributes (same formula:
 * damage + tier bonus, speed as given). Durability, enchantment value, repair ingredient, the
 * mineable-with-hoe Tool rules and the 2-per-hit wear come from the native TieredItem/DiggerItem.
 */
public class HoeItem26 extends HoeItem {

    public HoeItem26(Tier material, float attackDamage, float attackSpeed, Item.Properties properties) {
        super(material, props(material, attackDamage, attackSpeed, properties));
    }

    private static Item.Properties props(
            Tier material, float attackDamage, float attackSpeed, Item.Properties properties) {
        // 26.x marks every material item enchantable; 1.21.1 isEnchantable also wants MAX_DAMAGE,
        // which unbreakable (durability 0) materials lack -> the enchantable component restores it
        if (material.getEnchantmentValue() > 0)
            ItemProps26.enchantable(properties, material.getEnchantmentValue());
        return properties.attributes(DiggerItem.createAttributes(material, attackDamage, attackSpeed));
    }
}
