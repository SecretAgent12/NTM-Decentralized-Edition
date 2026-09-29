// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.armor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Backport of the 26.x {@code net.minecraft.world.item.equipment.ArmorMaterial} record (same components,
 * same {@link #createAttributes}).
 *
 * <p>Backport: every instance is also registered as a native 1.21.1 {@link net.minecraft.world.item.ArmorMaterial}
 * (Registries.ARMOR_MATERIAL, see {@link ArmorRegistry}) the moment it is constructed, so the
 * {@link ArmorItem26}s made from it are real 1.21.1 ArmorItems. The mod builds its materials in
 * ModItems' static initialiser, i.e. during mod construction, before NeoForge fires the armor-material
 * RegisterEvent (it runs right after the data-component one, before items). A material first built
 * after that is not registered and its items fall back to {@link ArmorRegistry#NONE}.
 */
public record ArmorMaterial(
        int durability,
        Map<ArmorType, Integer> defense,
        int enchantmentValue,
        Holder<SoundEvent> equipSound,
        float toughness,
        float knockbackResistance,
        TagKey<Item> repairIngredient,
        ResourceKey<EquipmentAsset> assetId) {

    public ArmorMaterial(
            int durability,
            Map<ArmorType, Integer> defense,
            int enchantmentValue,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            TagKey<Item> repairIngredient,
            ResourceKey<EquipmentAsset> assetId) {
        this.durability = durability;
        this.defense = Map.copyOf(defense);
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
        this.assetId = assetId;
        ArmorRegistry.registerMaterial(this);
    }

    public ItemAttributeModifiers createAttributes(ArmorType type) {
        ItemAttributeModifiers attributes = attributes(type);
        // backport: remembered so ArmorItem26 can tell which material its Properties were built from
        ArmorRegistry.bindAttributes(attributes, this, type);
        return attributes;
    }

    /** {@link #createAttributes} without the backport bookkeeping. */
    ItemAttributeModifiers attributes(ArmorType type) {
        int armor = defense.getOrDefault(type, 0);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
        ResourceLocation id = ResourceLocation.withDefaultNamespace("armor." + type.getName());
        builder.add(
                Attributes.ARMOR,
                new AttributeModifier(id, armor, AttributeModifier.Operation.ADD_VALUE),
                group);
        builder.add(
                Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(id, toughness, AttributeModifier.Operation.ADD_VALUE),
                group);
        if (knockbackResistance > 0.0F) {
            builder.add(
                    Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(id, knockbackResistance, AttributeModifier.Operation.ADD_VALUE),
                    group);
        }
        return builder.build();
    }

    /**
     * The registered 1.21.1 material for this one. {@code layered}: whether the vanilla armor layer
     * renders (26.x: the Equippable has an asset id); without it the 1.21.1 material has no layers.
     */
    public Holder<net.minecraft.world.item.ArmorMaterial> holder(boolean layered) {
        return ArmorRegistry.holder(this, layered);
    }

    /** This material as a 1.21.1 value. */
    net.minecraft.world.item.ArmorMaterial toVanilla(boolean layered) {
        Map<ArmorItem.Type, Integer> vanillaDefense = new EnumMap<>(ArmorItem.Type.class);
        defense.forEach((type, value) -> vanillaDefense.put(type.toVanilla(), value));
        TagKey<Item> repair = repairIngredient;
        List<net.minecraft.world.item.ArmorMaterial.Layer> layers =
                layered
                        ? List.of(new net.minecraft.world.item.ArmorMaterial.Layer(assetId.location()))
                        : List.of();
        return new net.minecraft.world.item.ArmorMaterial(
                vanillaDefense,
                enchantmentValue,
                equipSound,
                () -> ArmorMaterials.repairIngredient(repair),
                layers,
                toughness,
                knockbackResistance);
    }
}
