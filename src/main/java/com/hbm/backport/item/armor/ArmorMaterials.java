// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.armor;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Backport of 26.x {@code net.minecraft.world.item.equipment.ArmorMaterials} (the vanilla materials as
 * data templates; ntm-next derives its own from them).
 * backport: unverified: values reproduced from 1.21.2+ vanilla (they are unchanged since).
 */
public final class ArmorMaterials {

    // 26.x ItemTags.REPAIRS_*: the tags do not exist in 1.21.1 data, see repairIngredient
    public static final TagKey<Item> REPAIRS_LEATHER_ARMOR = tag("repairs_leather_armor");
    public static final TagKey<Item> REPAIRS_CHAIN_ARMOR = tag("repairs_chain_armor");
    public static final TagKey<Item> REPAIRS_IRON_ARMOR = tag("repairs_iron_armor");
    public static final TagKey<Item> REPAIRS_GOLD_ARMOR = tag("repairs_gold_armor");
    public static final TagKey<Item> REPAIRS_DIAMOND_ARMOR = tag("repairs_diamond_armor");
    public static final TagKey<Item> REPAIRS_TURTLE_HELMET = tag("repairs_turtle_helmet");
    public static final TagKey<Item> REPAIRS_NETHERITE_ARMOR = tag("repairs_netherite_armor");
    public static final TagKey<Item> REPAIRS_WOLF_ARMOR = tag("repairs_wolf_armor");

    public static final ArmorMaterial LEATHER =
            new ArmorMaterial(5, defense(1, 2, 3, 1, 3), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
                    REPAIRS_LEATHER_ARMOR, EquipmentAssets.LEATHER);
    public static final ArmorMaterial CHAINMAIL =
            new ArmorMaterial(15, defense(1, 4, 5, 2, 4), 12, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F,
                    REPAIRS_CHAIN_ARMOR, EquipmentAssets.CHAINMAIL);
    public static final ArmorMaterial IRON =
            new ArmorMaterial(15, defense(2, 5, 6, 2, 5), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
                    REPAIRS_IRON_ARMOR, EquipmentAssets.IRON);
    public static final ArmorMaterial GOLD =
            new ArmorMaterial(7, defense(1, 3, 5, 2, 7), 25, SoundEvents.ARMOR_EQUIP_GOLD, 0.0F, 0.0F,
                    REPAIRS_GOLD_ARMOR, EquipmentAssets.GOLD);
    public static final ArmorMaterial DIAMOND =
            new ArmorMaterial(33, defense(3, 6, 8, 3, 11), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.0F,
                    REPAIRS_DIAMOND_ARMOR, EquipmentAssets.DIAMOND);
    public static final ArmorMaterial TURTLE_SCUTE =
            new ArmorMaterial(25, defense(2, 5, 6, 2, 5), 9, SoundEvents.ARMOR_EQUIP_TURTLE, 0.0F, 0.0F,
                    REPAIRS_TURTLE_HELMET, EquipmentAssets.TURTLE_SCUTE);
    public static final ArmorMaterial NETHERITE =
            new ArmorMaterial(37, defense(3, 6, 8, 3, 19), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F,
                    REPAIRS_NETHERITE_ARMOR, EquipmentAssets.NETHERITE);
    public static final ArmorMaterial ARMADILLO_SCUTE =
            new ArmorMaterial(4, defense(3, 6, 8, 3, 11), 10, SoundEvents.ARMOR_EQUIP_WOLF, 0.0F, 0.0F,
                    REPAIRS_WOLF_ARMOR, EquipmentAssets.ARMADILLO_SCUTE);

    private ArmorMaterials() {}

    private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, leggings);
        map.put(ArmorType.CHESTPLATE, chestplate);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace(name));
    }

    /**
     * The 1.21.1 repair ingredient for a 26.x repair tag. 1.21.1 data has no {@code minecraft:repairs_*}
     * tags (vanilla 1.21.1 armor names the item directly), so those map to the item 1.21.1 uses;
     * every other tag (e.g. {@code hbm:armor_repair/*}) is used as is.
     */
    public static Ingredient repairIngredient(TagKey<Item> tag) {
        if (tag.location().getNamespace().equals("minecraft")) {
            switch (tag.location().getPath()) {
                case "repairs_leather_armor": return Ingredient.of(Items.LEATHER);
                case "repairs_chain_armor", "repairs_iron_armor": return Ingredient.of(Items.IRON_INGOT);
                case "repairs_gold_armor": return Ingredient.of(Items.GOLD_INGOT);
                case "repairs_diamond_armor": return Ingredient.of(Items.DIAMOND);
                case "repairs_turtle_helmet": return Ingredient.of(Items.TURTLE_SCUTE);
                case "repairs_netherite_armor": return Ingredient.of(Items.NETHERITE_INGOT);
                case "repairs_wolf_armor": return Ingredient.of(Items.ARMADILLO_SCUTE);
                default: break;
            }
        }
        return Ingredient.of(tag);
    }
}
