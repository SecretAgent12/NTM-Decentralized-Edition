// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.armor;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import org.jetbrains.annotations.Nullable;

/**
 * Backport of 26.x {@code net.minecraft.world.item.equipment.ArmorType} (1.21.1: {@link ArmorItem.Type}).
 */
public enum ArmorType implements StringRepresentable {
    HELMET(EquipmentSlot.HEAD, 11, "helmet"),
    CHESTPLATE(EquipmentSlot.CHEST, 16, "chestplate"),
    LEGGINGS(EquipmentSlot.LEGS, 15, "leggings"),
    BOOTS(EquipmentSlot.FEET, 13, "boots"),
    BODY(EquipmentSlot.BODY, 16, "body");

    public static final Codec<ArmorType> CODEC = StringRepresentable.fromValues(ArmorType::values);

    private final EquipmentSlot slot;
    private final int unitDurability;
    private final String name;

    ArmorType(EquipmentSlot slot, int unitDurability, String name) {
        this.slot = slot;
        this.unitDurability = unitDurability;
        this.name = name;
    }

    public int getDurability(int baseDurability) {
        return unitDurability * baseDurability;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public ArmorItem.Type toVanilla() {
        return switch (this) {
            case HELMET -> ArmorItem.Type.HELMET;
            case CHESTPLATE -> ArmorItem.Type.CHESTPLATE;
            case LEGGINGS -> ArmorItem.Type.LEGGINGS;
            case BOOTS -> ArmorItem.Type.BOOTS;
            case BODY -> ArmorItem.Type.BODY;
        };
    }

    public static @Nullable ArmorType bySlot(EquipmentSlot slot) {
        for (ArmorType type : values()) if (type.slot == slot) return type;
        return null;
    }
}
