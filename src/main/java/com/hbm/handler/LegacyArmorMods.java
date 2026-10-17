// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.NuclearTech;
import com.hbm.backport.Nbt;
import com.hbm.items.ModDataComponents;
import com.hbm.items.armor.ArmorMods;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * backport-fix: BF-093 armor mods from DE beta.1–10 (NEXT 1.0.0 layout) migrate to the 1.0.1
 * hbm:armor_mods component.
 *
 * <p>DE beta.1–beta.10 (NEXT 1.0.0) kept installed armor mods in the stack's {@code
 * hbm:persistent_data} custom data, under the compound {@value #MOD_COMPOUND_KEY} with one entry
 * {@value #MOD_SLOT_KEY}{@code <slot>} per socket, each an {@code ItemStack.CODEC} encoding (plain
 * {@code NbtOps}) of the mod with count 1. NEXT 1.0.1 moved them to {@link
 * ModDataComponents#ARMOR_MODS} and dropped the old data. The conversion runs lazily the first
 * time {@link ArmorModHandler#mods} looks at such a stack, keeps the slot numbers, and removes
 * the legacy compound so it never runs twice.
 */
public final class LegacyArmorMods {
    static final String MOD_COMPOUND_KEY = "ntm_armor_mods";
    static final String MOD_SLOT_KEY = "mod_slot_";

    private LegacyArmorMods() {}

    public static void migrate(ItemStack armor) {
        if (armor.isEmpty() || armor.has(ModDataComponents.ARMOR_MODS.get())) return;
        CustomData data = armor.get(ModDataComponents.PERSISTENT_DATA.get());
        if (data == null || !data.contains(MOD_COMPOUND_KEY)) return;

        CompoundTag root = data.copyTag();
        CompoundTag legacy = Nbt.getCompoundOrEmpty(root, MOD_COMPOUND_KEY);
        ArmorMods mods = ArmorMods.EMPTY;
        for (int slot = 0; slot < ArmorModHandler.MOD_SLOTS; slot++) {
            Tag encoded = legacy.get(MOD_SLOT_KEY + slot);
            if (encoded == null) continue;
            DataResult<ItemStack> parsed = ItemStack.CODEC.parse(NbtOps.INSTANCE, encoded);
            ItemStack mod = parsed.result().orElse(ItemStack.EMPTY);
            if (mod.isEmpty()) {
                NuclearTech.LOGGER.warn(
                        "Dropping unreadable legacy armor mod in slot {} of {}: {}",
                        slot,
                        armor.getItem(),
                        parsed.error().map(DataResult.Error::message).orElse("empty stack"));
                continue;
            }
            try {
                mods = mods.with(slot, mod);
            } catch (RuntimeException e) {
                NuclearTech.LOGGER.warn(
                        "Dropping invalid legacy armor mod {} in slot {} of {}: {}",
                        mod.getItem(),
                        slot,
                        armor.getItem(),
                        e.getMessage());
            }
        }

        root.remove(MOD_COMPOUND_KEY);
        CustomData.set(ModDataComponents.PERSISTENT_DATA.get(), armor, root);
        if (mods != ArmorMods.EMPTY) armor.set(ModDataComponents.ARMOR_MODS.get(), mods);
    }
}
