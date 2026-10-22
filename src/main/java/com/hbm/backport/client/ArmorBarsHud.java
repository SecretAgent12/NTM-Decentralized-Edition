// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ItemJetpack;
import com.hbm.items.armor.ModArmorItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * backport-fix: BF-110 (Discord) the thin bars above the armor row, as in 1.7.10
 * (ModEventHandlerClient.onHUDRenderBar / getBars): for every armor piece, the charge of powered armor (green, only
 * while the whole set is worn, charge ignored) and the fuel of a jetpack, worn or installed as an armor mod (in the
 * fuel's colour). One bar fills the 81 px of the row; with several, the first takes 40 px and the rest share the
 * other 40. Every piece with bars moves the left HUD column up by 4 px. NTM: NEXT had dropped this HUD.
 */
@EventBusSubscriber(modid = "hbm", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArmorBarsHud {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hbm", "armor_bars");
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    }; // 1.7.10 armorInventory order

    private ArmorBarsHud() {}

    private record Bar(float value, int color) {}

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, ID, (graphics, delta) -> render(graphics));
    }

    private static void bars(ItemStack stack, LocalPlayer player, List<Bar> out) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof ModArmorItem armor && armor.isPowered()
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate
                && ArmorSuitEffects.hasFullSet(player, plate.suit(), false)) {
            long max = armor.maxPower(stack);
            out.add(new Bar(max <= 0 ? 0F : (float) armor.getCharge(stack) / max, 0x00FF00));
        }
        if (stack.getItem() instanceof ItemJetpack jetpack) {
            int capacity = jetpack.capacity(stack);
            NTMFluidProperty fuel = NTMFluidProperties.get(jetpack.getContent(stack).type());
            out.add(new Bar(capacity <= 0 ? 0F : (float) jetpack.getFuel(stack) / capacity,
                    fuel == null ? 0xFFFFFF : fuel.color()));
        }
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;

        List<List<Bar>> perPiece = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            List<Bar> bars = new ArrayList<>();
            ItemStack armor = player.getItemBySlot(slot);
            bars(armor, player, bars);
            if (!armor.isEmpty() && ArmorModHandler.hasMods(armor)) {
                for (ItemStack mod : ArmorModHandler.pryMods(armor)) if (mod != null) bars(mod, player, bars);
            }
            perPiece.add(bars);
        }

        var gui = mc.gui;
        int left = graphics.guiWidth() / 2 - 91;
        for (List<Bar> bars : perPiece) {
            if (bars.isEmpty()) continue;
            int top = graphics.guiHeight() - gui.leftHeight + 7;
            for (int i = 0; i < bars.size(); i++) {
                int hstart, hend;
                if (i == 0) {
                    hstart = left;
                    hend = hstart + (bars.size() == 1 ? 81 : 40);
                } else {
                    int bl = (int) Math.ceil(40F / (bars.size() - 1));
                    hstart = left + 41 + bl * (i - 1);
                    hend = i == bars.size() - 1 ? left + 81 : hstart + bl;
                    if (i != 1) hstart += 1;
                }
                graphics.fill(hstart, top - 1, hend, top + 2, 0xFF404040);
                float value = Math.max(0F, Math.min(1F, bars.get(i).value()));
                int valx = (int) (hstart + (hend - hstart - 1) * value);
                if (valx > hstart + 1) graphics.fill(hstart + 1, top, valx, top + 1, 0xFF000000 | bars.get(i).color());
            }
            gui.leftHeight += 4;
        }
    }
}
