// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.create;

import com.hbm.NuclearTech;
import com.hbm.handler.ArmorModHandler;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * backport: Create's Engineer's Goggles as an NTM helmet mod (player request): put them into the
 * helmet's helmet slot or its extra slot on the armor modification table, and Create treats the
 * player as wearing goggles, so its info overlays work without taking the NTM helmet off.
 *
 * <p>Only called with Create installed. Create's item is looked up by id, so nothing here touches
 * Create's registration classes; the one Create call is its public goggles hook.
 */
public final class CreateGogglesCompat {

    private static Item goggles = Items.AIR;

    private CreateGogglesCompat() {}

    public static void register() {
        goggles = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "goggles"));
        if (goggles == Items.AIR) {
            NuclearTech.LOGGER.warn("Create is loaded but create:goggles is missing; goggles mod disabled");
            return;
        }
        ArmorModHandler.registerForeignMod(
                goggles,
                new ArmorModHandler.ForeignMod(
                        (1 << ArmorModHandler.HELMET_ONLY) | (1 << ArmorModHandler.EXTRA),
                        true,
                        false,
                        false,
                        false));
        GogglesItem.addIsWearingPredicate(CreateGogglesCompat::wearsGogglesMod);
    }

    private static boolean wearsGogglesMod(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !ArmorModHandler.hasMods(helmet)) return false;
        ItemStack[] mods = ArmorModHandler.pryMods(helmet);
        return mods[ArmorModHandler.HELMET_ONLY].is(goggles)
                || mods[ArmorModHandler.EXTRA].is(goggles);
    }
}
