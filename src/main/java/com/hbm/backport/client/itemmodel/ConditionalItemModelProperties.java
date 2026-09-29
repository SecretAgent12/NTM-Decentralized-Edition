// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 26.x {@code ConditionalItemModelProperties}: registry + the vanilla properties hbm uses. */
public final class ConditionalItemModelProperties {
    public static final IdMapper<MapCodec<? extends ConditionalItemModelProperty>> ID_MAPPER =
            new IdMapper<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final MapCodec<ConditionalItemModelProperty> MAP_CODEC =
            Codec.lazyInitialized(() -> ID_MAPPER.codec(ResourceLocation.CODEC))
                    .dispatchMap("property", ConditionalItemModelProperty::type, c -> (MapCodec) c);

    static {
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("has_component"), HasComponent.MAP_CODEC);
    }

    private ConditionalItemModelProperties() {}

    /** 26.x {@code HasComponent}. */
    public record HasComponent(DataComponentType<?> componentType, boolean ignoreDefault)
            implements ConditionalItemModelProperty {
        public static final MapCodec<HasComponent> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                DataComponentType.CODEC.fieldOf("component").forGetter(HasComponent::componentType),
                                                Codec.BOOL.optionalFieldOf("ignore_default", false).forGetter(HasComponent::ignoreDefault))
                                        .apply(i, HasComponent::new));

        @Override
        public boolean get(
                ItemStack stack,
                @Nullable ClientLevel level,
                @Nullable LivingEntity owner,
                int seed,
                ItemDisplayContext displayContext) {
            if (!ignoreDefault) return stack.has(componentType);
            var patched = stack.getComponentsPatch().get(componentType);
            return patched != null && patched.isPresent();
        }

        @Override
        public MapCodec<HasComponent> type() {
            return MAP_CODEC;
        }
    }
}
