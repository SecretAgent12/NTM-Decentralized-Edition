// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.item.ItemModelResolver}: stack -> render state. Items
 * with an item definition use its baked {@link ItemModel} ({@link ClientItems}); everything else
 * (vanilla / other mods) is wrapped from its 1.21.1 baked model by {@link LegacyItemModel}.
 * 26.x {@code Minecraft.getItemModelResolver()} and renderer contexts' {@code itemModelResolver()}
 * map to {@link #get()}.
 */
public class ItemModelResolver {
    private static final ItemModelResolver INSTANCE = new ItemModelResolver();

    public static ItemModelResolver get() {
        return INSTANCE;
    }

    public void updateForLiving(
            ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, LivingEntity entity) {
        updateForTopItem(
                output, item, displayContext, entity.level(), ItemOwner.of(entity), entity.getId() + displayContext.ordinal());
    }

    public void updateForNonLiving(
            ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, Entity entity) {
        updateForTopItem(output, item, displayContext, entity.level(), null, entity.getId());
    }

    public void updateForTopItem(
            ItemStackRenderState output,
            ItemStack item,
            ItemDisplayContext displayContext,
            @Nullable Level level,
            @Nullable ItemOwner owner,
            int seed) {
        output.clear();
        if (!item.isEmpty()) {
            output.displayContext = displayContext;
            appendItemLayers(output, item, displayContext, level, owner, seed);
        }
    }

    public void appendItemLayers(
            ItemStackRenderState output,
            ItemStack item,
            ItemDisplayContext displayContext,
            @Nullable Level level,
            @Nullable ItemOwner owner,
            int seed) {
        ClientLevel clientLevel = level instanceof ClientLevel cl ? cl : null;
        ItemModel model = ClientItems.model(item.getItem());
        if (model != null) {
            output.setOversizedInGui(ClientItems.oversizedInGui(item.getItem()));
            model.update(output, item, this, displayContext, clientLevel, owner, seed);
        } else {
            LegacyItemModel.INSTANCE.update(output, item, this, displayContext, clientLevel, owner, seed);
        }
    }
}
