// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 26.x {@code net.minecraft.world.item.enchantment.Repairable} as the data component
 * {@code hbm:backport_repairable}. 1.21.1: answers Item.isValidRepairItem (MixinItemToolShims).
 */
public record Repairable(HolderSet<Item> items) {

    public static final Codec<Repairable> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            RegistryCodecs.homogeneousList(Registries.ITEM)
                                                    .fieldOf("items")
                                                    .forGetter(Repairable::items))
                                    .apply(i, Repairable::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Repairable> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.holderSet(Registries.ITEM), Repairable::items, Repairable::new);

    public boolean isValidRepairItem(ItemStack repairItem) {
        return items.contains(repairItem.getItemHolder());
    }
}
