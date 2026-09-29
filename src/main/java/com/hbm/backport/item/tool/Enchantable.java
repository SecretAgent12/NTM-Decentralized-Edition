// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * 26.x {@code net.minecraft.world.item.enchantment.Enchantable} as the data component
 * {@code hbm:backport_enchantable}. 1.21.1: the item's default value answers
 * Item.getEnchantmentValue() and makes Item.isEnchantable(stack) true (MixinItemToolShims).
 */
public record Enchantable(int value) {

    public static final Codec<Enchantable> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(ExtraCodecs.POSITIVE_INT.fieldOf("value").forGetter(Enchantable::value))
                                    .apply(i, Enchantable::new));

    public static final StreamCodec<ByteBuf, Enchantable> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(Enchantable::new, Enchantable::value);

    public Enchantable {
        if (value <= 0) throw new IllegalArgumentException("Enchantment value must be positive, but was " + value);
    }
}
