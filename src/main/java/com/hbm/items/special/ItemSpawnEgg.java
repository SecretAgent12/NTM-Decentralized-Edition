// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import com.hbm.backport.compat.SpawnEggItemCompat;

// backport: NeoForge 1.21.1's DeferredSpawnEggItem (type resolved late, colours given)
public class ItemSpawnEgg extends net.neoforged.neoforge.common.DeferredSpawnEggItem {

    @SuppressWarnings("unchecked")
    public ItemSpawnEgg(
            java.util.function.Supplier<? extends java.util.function.Supplier<? extends EntityType<?>>> type,
            int primary,
            int secondary,
            Properties properties) {
        super(
                () -> (EntityType<? extends net.minecraft.world.entity.Mob>) type.get().get(),
                primary,
                secondary,
                properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> type = getType(stack);
        return Component.translatable(
                getDescriptionId(), type == null ? "" : type.getDescription());
    }
}
