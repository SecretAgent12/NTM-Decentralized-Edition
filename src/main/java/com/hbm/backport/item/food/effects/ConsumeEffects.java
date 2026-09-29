// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
import java.util.Map;

/** The dispatch codec over the backported consume effect types (26.x ConsumeEffect.CODEC). */
public final class ConsumeEffects {

    private static final Map<String, ConsumeEffect.Type<?>> TYPES = new LinkedHashMap<>();

    static {
        add(ApplyStatusEffectsConsumeEffect.TYPE);
        add(RemoveStatusEffectsConsumeEffect.TYPE);
        add(ClearAllStatusEffectsConsumeEffect.TYPE);
        add(TeleportRandomlyConsumeEffect.TYPE);
        add(PlaySoundConsumeEffect.TYPE);
    }

    private static final Codec<ConsumeEffect.Type<?>> TYPE_CODEC =
            Codec.STRING.comapFlatMap(
                    id -> {
                        ConsumeEffect.Type<?> type = TYPES.get(id);
                        return type != null
                                ? DataResult.<ConsumeEffect.Type<?>>success(type)
                                : DataResult.<ConsumeEffect.Type<?>>error(() -> "Unknown consume effect type: " + id);
                    },
                    ConsumeEffect.Type::id);

    public static final Codec<ConsumeEffect> CODEC =
            TYPE_CODEC.dispatch("type", ConsumeEffect::getType, ConsumeEffects::codecOf);

    private ConsumeEffects() {}

    private static void add(ConsumeEffect.Type<?> type) {
        TYPES.put(type.id(), type);
    }

    private static MapCodec<? extends ConsumeEffect> codecOf(ConsumeEffect.Type<?> type) {
        return type.codec();
    }
}
