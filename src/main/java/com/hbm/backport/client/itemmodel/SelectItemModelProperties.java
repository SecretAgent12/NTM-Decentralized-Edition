// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties}: the
 * select property registry, with the vanilla properties the hbm item definitions use
 * (display_context, component, block_state).
 */
public final class SelectItemModelProperties {
    public static final IdMapper<SelectItemModelProperty.Type<?, ?>> ID_MAPPER = new IdMapper<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> CODEC =
            (MapCodec)
                    Codec.lazyInitialized(() -> ID_MAPPER.codec(ResourceLocation.CODEC))
                            .dispatchMap(
                                    "property",
                                    s -> ((SelectItemModel.UnbakedSwitch) s).property().type(),
                                    t -> ((SelectItemModelProperty.Type) t).switchCodec());

    static {
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("display_context"), DisplayContext.TYPE);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("block_state"), BlockState.TYPE);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("component"), ComponentContents.TYPE);
    }

    private SelectItemModelProperties() {}

    /**
     * 26.x {@code DisplayContext}. backport: values are kept as serialized names so 26.x-only
     * contexts (e.g. "on_shelf") in "when" lists decode and simply never match.
     */
    public record DisplayContext() implements SelectItemModelProperty<String> {
        public static final SelectItemModelProperty.Type<DisplayContext, String> TYPE =
                SelectItemModelProperty.Type.create(MapCodec.unit(new DisplayContext()), Codec.STRING);

        @Override
        public String get(
                ItemStack stack,
                @Nullable ClientLevel level,
                @Nullable LivingEntity owner,
                int seed,
                ItemDisplayContext displayContext) {
            return displayContext.getSerializedName();
        }

        @Override
        public Codec<String> valueCodec() {
            return Codec.STRING;
        }

        @Override
        public SelectItemModelProperty.Type<DisplayContext, String> type() {
            return TYPE;
        }
    }

    /** 26.x {@code ItemBlockState}: a property of the stack's block_state component. */
    public record BlockState(String property) implements SelectItemModelProperty<String> {
        public static final SelectItemModelProperty.Type<BlockState, String> TYPE =
                SelectItemModelProperty.Type.create(
                        RecordCodecBuilder.mapCodec(
                                i -> i.group(Codec.STRING.fieldOf("block_state_property").forGetter(BlockState::property))
                                        .apply(i, BlockState::new)),
                        Codec.STRING);

        @Override
        public @Nullable String get(
                ItemStack stack,
                @Nullable ClientLevel level,
                @Nullable LivingEntity owner,
                int seed,
                ItemDisplayContext displayContext) {
            BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
            return props == null ? null : props.properties().get(property);
        }

        @Override
        public Codec<String> valueCodec() {
            return Codec.STRING;
        }

        @Override
        public SelectItemModelProperty.Type<BlockState, String> type() {
            return TYPE;
        }
    }

    /** 26.x {@code ComponentContents}: the value of a data component, cases decoded with its codec. */
    public record ComponentContents<T>(DataComponentType<T> componentType)
            implements SelectItemModelProperty<T> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static final SelectItemModelProperty.Type<ComponentContents<Object>, Object> TYPE =
                new SelectItemModelProperty.Type<>(
                        (MapCodec)
                                DataComponentType.CODEC.dispatchMap(
                                        "component",
                                        s -> ((ComponentContents) ((SelectItemModel.UnbakedSwitch) s).property()).componentType(),
                                        c -> switchFor((DataComponentType) c)));

        private static <T> MapCodec<SelectItemModel.UnbakedSwitch<ComponentContents<T>, T>> switchFor(
                DataComponentType<T> type) {
            ComponentContents<T> property = new ComponentContents<>(type);
            return SelectItemModelProperty.Type.create(MapCodec.unit(property), type.codecOrThrow())
                    .switchCodec();
        }

        @Override
        public @Nullable T get(
                ItemStack stack,
                @Nullable ClientLevel level,
                @Nullable LivingEntity owner,
                int seed,
                ItemDisplayContext displayContext) {
            return stack.get(componentType);
        }

        @Override
        public Codec<T> valueCodec() {
            return componentType.codecOrThrow();
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public SelectItemModelProperty.Type<ComponentContents<T>, T> type() {
            return (SelectItemModelProperty.Type) TYPE;
        }
    }
}
