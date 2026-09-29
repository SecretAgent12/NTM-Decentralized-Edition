// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import org.jspecify.annotations.Nullable;

/** 26.x {@code RangeSelectItemModelProperties}: registry + the compass angle. */
public final class RangeSelectItemModelProperties {
    public static final IdMapper<MapCodec<? extends RangeSelectItemModelProperty>> ID_MAPPER =
            new IdMapper<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final MapCodec<RangeSelectItemModelProperty> MAP_CODEC =
            Codec.lazyInitialized(() -> ID_MAPPER.codec(ResourceLocation.CODEC))
                    .dispatchMap("property", RangeSelectItemModelProperty::type, c -> (MapCodec) c);

    static {
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("compass"), CompassAngle.MAP_CODEC);
    }

    private RangeSelectItemModelProperties() {}

    public enum CompassTarget implements StringRepresentable {
        NONE("none"),
        LODESTONE("lodestone"),
        SPAWN("spawn"),
        RECOVERY("recovery");

        public static final Codec<CompassTarget> CODEC = StringRepresentable.fromEnum(CompassTarget::values);
        private final String name;

        CompassTarget(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * 26.x {@code CompassAngle}: backport: evaluated by 1.21.1's {@link CompassItemPropertyFunction}
     * (same 0..1 angle, wobble always on) for the chosen target.
     */
    public record CompassAngle(boolean wobble, CompassTarget target) implements RangeSelectItemModelProperty {
        public static final MapCodec<CompassAngle> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                Codec.BOOL.optionalFieldOf("wobble", true).forGetter(CompassAngle::wobble),
                                                CompassTarget.CODEC.fieldOf("target").forGetter(CompassAngle::target))
                                        .apply(i, CompassAngle::new));

        private CompassItemPropertyFunction function() {
            return FUNCTIONS[target.ordinal()];
        }

        private static final CompassItemPropertyFunction[] FUNCTIONS = {
            new CompassItemPropertyFunction((level, stack, entity) -> null),
            new CompassItemPropertyFunction(
                    (level, stack, entity) -> {
                        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                        return tracker != null ? tracker.target().orElse(null) : null;
                    }),
            new CompassItemPropertyFunction((level, stack, entity) -> CompassItem.getSpawnPosition(level)),
            new CompassItemPropertyFunction(
                    (level, stack, entity) ->
                            entity instanceof Player player ? player.getLastDeathLocation().orElse(null) : null)
        };

        @Override
        public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
            LivingEntity living = owner != null ? owner.asLivingEntity() : null;
            return function().unclampedCall(stack, level, living, seed);
        }

        @Override
        public MapCodec<CompassAngle> type() {
            return MAP_CODEC;
        }
    }
}
