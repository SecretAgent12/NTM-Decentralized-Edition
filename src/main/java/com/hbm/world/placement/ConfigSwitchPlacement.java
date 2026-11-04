// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.world.placement;

import com.hbm.config.ConfigEntry;
import com.hbm.config.ConfigSchema;
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * backport: placement filter {@code hbm:config_switch} (not in ntm-next or 1.7.10). A world object
 * (crashed bomb, meteorite, safe, geyser...) generates only while its line in hbm-runtime.toml
 * {@code [world_objects]} is on: {@code {"type": "hbm:config_switch", "switch": "duds"}} first in
 * the placed feature's placement list.
 *
 * <p>Unlike the datapack's {@code hbm:world_config} switches (ores), these live in the mod's own
 * config file, one for all worlds, where server owners look for them. The line is read when a chunk
 * generates, so a change applies to chunks generated after it without a restart; what is already in
 * the world stays.
 */
public final class ConfigSwitchPlacement extends PlacementFilter {

    public static final MapCodec<ConfigSwitchPlacement> CODEC =
            Codec.STRING
                    .comapFlatMap(ConfigSwitchPlacement::lookup, ConfigEntry::key)
                    .fieldOf("switch")
                    .xmap(ConfigSwitchPlacement::new, placement -> placement.line);
    private static RegistryHandle<PlacementModifierType<ConfigSwitchPlacement>> type;

    private final ConfigEntry<Boolean> line;

    public ConfigSwitchPlacement(ConfigEntry<Boolean> line) {
        this.line = line;
    }

    public static void register() {
        type = NeoForgeRegistrar.registerPlacementModifierType("config_switch", CODEC);
    }

    private static DataResult<ConfigEntry<Boolean>> lookup(String key) {
        List<ConfigEntry<Boolean>> lines = ConfigSchema.WORLD_OBJECTS;
        for (int i = 0x0, n = lines.size(); i < n; i++) {
            ConfigEntry<Boolean> line = lines.get(i);
            if (key.equals(line.key())) return DataResult.success(line);
        }
        return DataResult.error(() -> "no [world_objects] line " + key + " in hbm-runtime.toml");
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return Boolean.TRUE.equals(NeoForgeHbmConfig.STORE.get(line));
    }

    @Override
    public PlacementModifierType<?> type() {
        return type.get();
    }
}
