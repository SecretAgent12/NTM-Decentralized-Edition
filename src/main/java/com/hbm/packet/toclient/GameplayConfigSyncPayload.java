// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * backport: the hbm-gameplay.toml lines a server changed from their defaults (path -> value as
 * JSON), for its remote players; see com.hbm.data.GameplayConfig. Not in ntm-next.
 */
public record GameplayConfigSyncPayload(Map<String, String> lines) implements CustomPacketPayload {

    public static final Type<GameplayConfigSyncPayload> TYPE =
            new Type<>(Library.id("gameplay_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GameplayConfigSyncPayload>
            STREAM_CODEC =
                    ByteBufCodecs.fromCodecWithRegistries(wire())
                            .map(GameplayConfigSyncPayload::new, GameplayConfigSyncPayload::lines);

    public GameplayConfigSyncPayload {
        lines = Map.copyOf(lines == null ? Map.of() : lines);
    }

    private static Codec<Map<String, String>> wire() {
        return Codec.unboundedMap(Codec.STRING, Codec.STRING);
    }

    @Override
    public Type<GameplayConfigSyncPayload> type() {
        return TYPE;
    }
}
