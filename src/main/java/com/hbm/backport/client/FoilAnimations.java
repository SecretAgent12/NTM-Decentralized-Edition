// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-112 (Discord #6) animated item icons (spiral drill, electric chainsaw, ...) froze while a tool ability
 * was active. The enchantment glint itself is 1.7.10 behaviour (ItemToolAbility.hasEffect: glint while a preset is
 * selected). The freeze comes from Sodium's "Animate Only Visible Textures": it advances a sprite only if something
 * marked it as drawn, and items drawn with the glint go through a path that does not mark them (switching the option
 * off fixes it, checked in game). Every client tick the sprites of glinting items carried by players (inventory,
 * hands, armor) are marked as drawn through Sodium's (or Embeddium's) public SpriteUtil, looked up reflectively; without
 * either mod nothing happens.
 */
@EventBusSubscriber(modid = "hbm", value = Dist.CLIENT)
public final class FoilAnimations {

    private static final @Nullable MethodHandle MARK = findMarker();

    private FoilAnimations() {}

    private static @Nullable MethodHandle findMarker() {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType type = MethodType.methodType(void.class, TextureAtlasSprite.class);
        try { // Sodium 0.6+: SpriteUtil.INSTANCE.markSpriteActive(sprite)
            Class<?> api = Class.forName("net.caffeinemc.mods.sodium.api.texture.SpriteUtil");
            Object instance = api.getField("INSTANCE").get(null);
            return lookup.findVirtual(api, "markSpriteActive", type).bindTo(instance);
        } catch (Throwable ignored) {
        }
        try { // Embeddium: SpriteUtil.markSpriteActive(sprite)
            Class<?> api = Class.forName("org.embeddedt.embeddium.api.render.texture.SpriteUtil");
            return lookup.findStatic(api, "markSpriteActive", type);
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        if (MARK == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) mark(mc, inventory.getItem(i), mc.player);
        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            for (EquipmentSlot slot : EquipmentSlot.values()) mark(mc, player.getItemBySlot(slot), player);
        }
    }

    private static void mark(Minecraft mc, ItemStack stack, Player holder) {
        if (stack.isEmpty() || !stack.hasFoil()) return;
        TextureAtlasSprite sprite =
                mc.getItemRenderer().getModel(stack, mc.level, holder, 0).getParticleIcon();
        if (sprite == null || sprite.contents().getUniqueFrames().count() <= 1) return;
        try {
            MARK.invokeExact(sprite);
        } catch (Throwable ignored) {
        }
    }
}
