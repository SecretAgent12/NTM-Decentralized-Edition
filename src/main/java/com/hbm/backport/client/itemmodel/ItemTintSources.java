// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.color.item.ItemTintSources}: the tint source type registry. */
public final class ItemTintSources {
    public static final IdMapper<MapCodec<? extends ItemTintSource>> ID_MAPPER = new IdMapper<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<ItemTintSource> CODEC =
            Codec.lazyInitialized(
                    () ->
                            ID_MAPPER.codec(ResourceLocation.CODEC)
                                    .dispatch(
                                            "type",
                                            ItemTintSource::type,
                                            c -> (MapCodec) c));

    static {
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("constant"), Constant.MAP_CODEC);
    }

    private ItemTintSources() {}

    /** 26.x {@code net.minecraft.client.color.item.Constant}. */
    public record Constant(int value) implements ItemTintSource {
        // backport-fix: BF-008 26.x reads "value" as an RGB color and forces full alpha; 1.21.1 item
        // rendering honours the tint alpha, so a raw 0xRRGGBB made tinted layers (spawn eggs) invisible
        public Constant {
            value = com.hbm.backport.ARGB.opaque(value);
        }

        public static final MapCodec<Constant> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i -> i.group(Codec.INT.fieldOf("value").forGetter(Constant::value))
                                .apply(i, Constant::new));

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            return value;
        }

        @Override
        public MapCodec<Constant> type() {
            return MAP_CODEC;
        }
    }
}
