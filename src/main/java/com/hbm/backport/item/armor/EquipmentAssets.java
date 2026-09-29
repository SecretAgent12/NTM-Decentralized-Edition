// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.armor;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Backport of 26.x {@code net.minecraft.world.item.equipment.EquipmentAssets}. 1.21.1 has no equipment
 * assets; the ids are only carried to pick the armor layer texture (see {@link ArmorItem26}).
 */
public final class EquipmentAssets {

    public static final ResourceKey<? extends Registry<EquipmentAsset>> ROOT_ID =
            ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("equipment_asset"));

    public static final ResourceKey<EquipmentAsset> LEATHER = createId("leather");
    public static final ResourceKey<EquipmentAsset> CHAINMAIL = createId("chainmail");
    public static final ResourceKey<EquipmentAsset> IRON = createId("iron");
    public static final ResourceKey<EquipmentAsset> GOLD = createId("gold");
    public static final ResourceKey<EquipmentAsset> DIAMOND = createId("diamond");
    public static final ResourceKey<EquipmentAsset> TURTLE_SCUTE = createId("turtle_scute");
    public static final ResourceKey<EquipmentAsset> NETHERITE = createId("netherite");
    public static final ResourceKey<EquipmentAsset> ARMADILLO_SCUTE = createId("armadillo_scute");
    public static final ResourceKey<EquipmentAsset> ELYTRA = createId("elytra");

    private EquipmentAssets() {}

    static ResourceKey<EquipmentAsset> createId(String name) {
        return ResourceKey.create(ROOT_ID, ResourceLocation.withDefaultNamespace(name));
    }
}
