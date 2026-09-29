// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.armor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Registry side of the armor backport: the 1.21.1 armor materials behind the 26.x-style
 * {@link ArmorMaterial}s, the {@link Equippable} component type, and the damageOnHurt hook.
 *
 * <p>Each hbm material is registered twice: {@code hbm:<asset>} with one armor layer (26.x: the
 * item's Equippable names that asset) and {@code hbm:<asset>_no_layers} without (26.x: no asset, i.e.
 * OBJ-model suits and bespoke head gear -- the vanilla armor layer draws nothing for them).
 * Materials whose asset is a vanilla one resolve to the vanilla 1.21.1 material.
 */
public final class ArmorRegistry {

    private static final String MOD_ID = "hbm";

    private static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MOD_ID);

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);

    /** Material of armor items whose 26.x material is unknown: no defense, no layers, not repairable. */
    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial, net.minecraft.world.item.ArmorMaterial>
            NONE =
                    MATERIALS.register(
                            "backport_none",
                            () ->
                                    new net.minecraft.world.item.ArmorMaterial(
                                            Map.of(),
                                            0,
                                            SoundEvents.ARMOR_EQUIP_GENERIC,
                                            () -> Ingredient.EMPTY,
                                            List.of(),
                                            0.0F,
                                            0.0F));

    static {
        COMPONENTS.register("backport_equippable", () -> Equippable.TYPE);
    }

    private record Holders(
            Holder<net.minecraft.world.item.ArmorMaterial> layered,
            Holder<net.minecraft.world.item.ArmorMaterial> bare) {}

    /** What a createAttributes result was made from. */
    record Binding(ArmorMaterial material, ArmorType type) {}

    private static final Map<ArmorMaterial, Holders> HOLDERS = new HashMap<>();
    private static final Map<ResourceLocation, ArmorMaterial> BY_ASSET = new HashMap<>();
    private static final Set<String> NAMES = new HashSet<>();
    private static final Map<ItemAttributeModifiers, Binding> ATTRIBUTES = new IdentityHashMap<>();

    private ArmorRegistry() {}

    public static void register(IEventBus modBus) {
        MATERIALS.register(modBus);
        COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(ArmorRegistry::onArmorHurt);
    }

    static synchronized void registerMaterial(ArmorMaterial material) {
        if (HOLDERS.containsKey(material)) return;
        ResourceLocation asset = material.assetId().location();
        BY_ASSET.putIfAbsent(asset, material);
        Holder<net.minecraft.world.item.ArmorMaterial> vanilla = vanillaMaterial(asset);
        if (vanilla != null) {
            // backport: the vanilla material renders the vanilla layer; no asset-less variant needed
            HOLDERS.put(material, new Holders(vanilla, vanilla));
            return;
        }
        String base = (asset.getNamespace().equals(MOD_ID) ? "" : asset.getNamespace() + "_") + asset.getPath();
        String name = base;
        for (int i = 2; NAMES.contains(name) || NAMES.contains(name + "_no_layers"); i++) name = base + "_" + i;
        try {
            Holder<net.minecraft.world.item.ArmorMaterial> layered =
                    MATERIALS.register(name, () -> material.toVanilla(true));
            Holder<net.minecraft.world.item.ArmorMaterial> bare =
                    MATERIALS.register(name + "_no_layers", () -> material.toVanilla(false));
            NAMES.add(name);
            NAMES.add(name + "_no_layers");
            HOLDERS.put(material, new Holders(layered, bare));
        } catch (IllegalStateException registryClosed) {
            // backport: built after the armor-material registry event; its items use NONE
        }
    }

    private static @Nullable Holder<net.minecraft.world.item.ArmorMaterial> vanillaMaterial(ResourceLocation asset) {
        if (!asset.getNamespace().equals("minecraft")) return null;
        String path =
                switch (asset.getPath()) {
                    case "turtle_scute" -> "turtle";
                    case "armadillo_scute" -> "armadillo";
                    default -> asset.getPath();
                };
        return BuiltInRegistries.ARMOR_MATERIAL
                .getHolder(ResourceKey.create(Registries.ARMOR_MATERIAL, ResourceLocation.withDefaultNamespace(path)))
                .<Holder<net.minecraft.world.item.ArmorMaterial>>map(h -> h)
                .orElse(null);
    }

    static synchronized Holder<net.minecraft.world.item.ArmorMaterial> holder(ArmorMaterial material, boolean layered) {
        Holders holders = HOLDERS.get(material);
        if (holders == null) return NONE;
        return layered ? holders.layered() : holders.bare();
    }

    static synchronized @Nullable ArmorMaterial byAsset(ResourceKey<EquipmentAsset> asset) {
        return BY_ASSET.get(asset.location());
    }

    static synchronized void bindAttributes(ItemAttributeModifiers attributes, ArmorMaterial material, ArmorType type) {
        ATTRIBUTES.put(attributes, new Binding(material, type));
    }

    static synchronized @Nullable Binding binding(@Nullable ItemAttributeModifiers attributes) {
        return attributes == null ? null : ATTRIBUTES.get(attributes);
    }

    /** 26.x Equippable.damageOnHurt == false: the piece takes no durability damage from hits. */
    private static void onArmorHurt(ArmorHurtEvent event) {
        for (Map.Entry<EquipmentSlot, ArmorHurtEvent.ArmorEntry> entry : event.getArmorMap().entrySet()) {
            ItemStack stack = entry.getValue().armorItemStack;
            if (stack == null || stack.isEmpty()) continue;
            Equippable equippable = stack.get(Equippable.TYPE);
            if (equippable != null && !equippable.damageOnHurt()) entry.getValue().newDamage = 0.0F;
        }
    }
}
