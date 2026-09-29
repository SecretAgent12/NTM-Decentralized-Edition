// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty}. */
public interface SelectItemModelProperty<T> {
    @Nullable T get(
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity owner,
            int seed,
            ItemDisplayContext displayContext);

    Codec<T> valueCodec();

    Type<? extends SelectItemModelProperty<T>, T> type();

    record Type<P extends SelectItemModelProperty<T>, T>(
            MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec) {

        public static <P extends SelectItemModelProperty<T>, T> Type<P, T> create(
                MapCodec<P> propertyMapCodec, Codec<T> valueCodec) {
            Codec<SelectItemModel.SwitchCase<T>> caseCodec = SelectItemModel.SwitchCase.codec(valueCodec);
            MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec =
                    RecordCodecBuilder.mapCodec(
                            i ->
                                    i.group(
                                                    propertyMapCodec.forGetter(
                                                            SelectItemModel.UnbakedSwitch::property),
                                                    caseCodec
                                                            .listOf()
                                                            .fieldOf("cases")
                                                            .forGetter(SelectItemModel.UnbakedSwitch::cases))
                                            .apply(i, SelectItemModel.UnbakedSwitch::new));
            return new Type<>(switchCodec);
        }
    }
}
