// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.armor;

import com.hbm.backport.compat.ArmorItemCompat;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.Nullable;

/**
 * Base of the mod's armor (ModArmorItem): a native 1.21.1 {@link ArmorItem} built from 26.x-style
 * Item.Properties (Equippable component + armor attributes, no material argument).
 *
 * <p>26.x armor is a plain Item whose behaviour comes from its components, so its constructor only
 * takes Properties. Here the material and type are recovered after {@code super(...)} from the
 * built components: the ATTRIBUTE_MODIFIERS value made by {@link ArmorMaterial#createAttributes}
 * (remembered by identity), else the Equippable's asset id, else {@link ArmorRegistry#NONE}. ArmorItem's
 * own material/type fields hold placeholders; every ArmorItem method that reads them is overridden.
 */
public abstract class ArmorItem26 extends ArmorItemCompat {

    private final @Nullable ArmorMaterial armorMaterial;
    private final ArmorType armorType;
    private final @Nullable Equippable equippable;
    private final Holder<net.minecraft.world.item.ArmorMaterial> vanillaMaterial;

    public ArmorItem26(Item.Properties properties) {
        super(ArmorRegistry.NONE, ArmorItem.Type.CHESTPLATE, properties);
        DataComponentMap components = components();
        Equippable equippable = components.get(Equippable.TYPE);
        ArmorRegistry.Binding binding = ArmorRegistry.binding(components.get(DataComponents.ATTRIBUTE_MODIFIERS));
        ArmorMaterial material =
                binding != null
                        ? binding.material()
                        : equippable == null
                                ? null
                                : equippable.assetId().map(ArmorRegistry::byAsset).orElse(null);
        ArmorType type = binding != null ? binding.type() : null;
        if (type == null && equippable != null) type = ArmorType.bySlot(equippable.slot());
        this.armorMaterial = material;
        this.armorType = type == null ? ArmorType.CHESTPLATE : type;
        this.equippable = equippable;
        boolean layered = equippable != null && equippable.assetId().isPresent();
        this.vanillaMaterial = material == null ? ArmorRegistry.NONE : material.holder(layered);
    }

    /** The 26.x material this armor was built from, if known. */
    public @Nullable ArmorMaterial armorMaterial() {
        return armorMaterial;
    }

    public ArmorType armorType() {
        return armorType;
    }

    @Override
    public Holder<net.minecraft.world.item.ArmorMaterial> getMaterial() {
        return vanillaMaterial;
    }

    @Override
    public ArmorItem.Type getType() {
        return armorType.toVanilla();
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return equippable != null ? equippable.slot() : armorType.getSlot();
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        if (equippable != null) return equippable.equipSound();
        return armorMaterial != null ? armorMaterial.equipSound() : SoundEvents.ARMOR_EQUIP_GENERIC;
    }

    @Override
    public int getEnchantmentValue() {
        return armorMaterial != null ? armorMaterial.enchantmentValue() : 0;
    }

    /** 26.x: enchantable at the table iff the ENCHANTABLE component (material enchantability > 0) is set. */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return getEnchantmentValue() > 0 && stack.getMaxStackSize() == 1;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairItem) {
        return armorMaterial != null
                && ArmorMaterials.repairIngredient(armorMaterial.repairIngredient()).test(repairItem);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return armorMaterial != null ? armorMaterial.attributes(armorType) : ItemAttributeModifiers.EMPTY;
    }

    @Override
    public int getDefense() {
        return armorMaterial != null ? armorMaterial.defense().getOrDefault(armorType, 0) : 0;
    }

    @Override
    public float getToughness() {
        return armorMaterial != null ? armorMaterial.toughness() : 0.0F;
    }

    /**
     * 26.x draws the layer from {@code assets/<ns>/equipment/<asset>.json}, whose textures live at
     * {@code textures/entity/equipment/humanoid[_leggings]/<texture>.png}. All of ntm-next's equipment
     * assets name the texture after the asset, so the 26.x files are used directly.
     * backport: unverified for assets added later whose json names a different texture.
     */
    @Override
    public @Nullable ResourceLocation getArmorTexture(
            ItemStack stack,
            Entity entity,
            EquipmentSlot slot,
            net.minecraft.world.item.ArmorMaterial.Layer layer,
            boolean innerModel) {
        if (equippable != null && equippable.assetId().isPresent()) {
            ResourceKey<EquipmentAsset> asset = equippable.assetId().get();
            ResourceLocation id = asset.location();
            if (!id.getNamespace().equals("minecraft")) {
                return ResourceLocation.fromNamespaceAndPath(
                        id.getNamespace(),
                        "textures/entity/equipment/"
                                + (innerModel ? "humanoid_leggings" : "humanoid")
                                + "/"
                                + id.getPath()
                                + ".png");
            }
        }
        return super.getArmorTexture(stack, entity, slot, layer, innerModel);
    }
}
