// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.armor;

import net.minecraft.world.item.Item;

/** 26.x Item.Properties methods for armor (call sites are rewritten to these). */
public final class ArmorProps {

    private ArmorProps() {}

    /**
     * 26.x {@code Item.Properties.humanoidArmor(material, type)}.
     * backport: 26.x also sets the ENCHANTABLE and REPAIRABLE components here; the item this is used for
     * is an {@link ArmorItem26}, whose registered 1.21.1 material supplies enchantability and repair.
     */
    public static Item.Properties humanoidArmor(Item.Properties properties, ArmorMaterial material, ArmorType type) {
        return properties
                .durability(type.getDurability(material.durability()))
                .attributes(material.createAttributes(type))
                .component(
                        Equippable.TYPE,
                        Equippable.builder(type.getSlot())
                                .setEquipSound(material.equipSound())
                                .setAsset(material.assetId())
                                .build());
    }
}
