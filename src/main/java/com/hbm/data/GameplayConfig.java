// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.data;

import com.google.gson.JsonParser;
import com.hbm.handler.radiation.RadiationSettings;
import com.hbm.packet.toclient.GameplayConfigSyncPayload;
import com.hbm.platform.NeoForgeNetwork;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

/**
 * backport: hbm-gameplay.toml (not in ntm-next, where all of this is datapack only; 1.7.10 kept it
 * in hbm.cfg). The datapack's gameplay groups - explosion_config, mob_config, radiation_config,
 * item_config, world_config and machine_config/machines - become lines of an ordinary config file,
 * one section per group, plus the Nether's background radiation (radiation_settings/the_nether).
 *
 * <p>The datapack still loads first. A line changed here from its default replaces the datapack's
 * value; a line left at the default keeps it, so world datapacks of existing packs work as before.
 * Values are checked with the datapack's own codecs: a value the datapack would refuse is logged and
 * ignored. A client on a remote server takes the server's changed lines ({@link
 * GameplayConfigSyncPayload}, sent on join, on /reload and when the file changes), never its own
 * file. Most values apply at once; the few read when a world loads (the Nether's radiation among
 * them) wait for the next start.
 */
public final class GameplayConfig {

    public static final String FILE = "hbm-gameplay.toml";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** radiation_settings/the_nether.json ambient_rad, 1.7.10's AMBIENT_00_nether. */
    private static final double NETHER_AMBIENT_RAD = 0x1.999999999999ap-4D;

    private record Line(String path, DataGroups.Param<?> param, ModConfigSpec.ConfigValue<?> value) {}

    private static final List<Line> LINES = new ArrayList<>();
    /** What the datapack gave each mirrored parameter, before this file is laid over it. */
    private static final Map<DataGroups.Param<?>, Object> DATAPACK = new IdentityHashMap<>();
    private static final Set<String> REPORTED = new HashSet<>();
    private static ModConfigSpec.DoubleValue netherAmbientRad;
    /** On a remote server: the server's changed lines (path -> JSON); null everywhere else. */
    private static volatile Map<String, String> remote;

    public static final ModConfigSpec SPEC = build();

    public static final NeoForgeNetwork.DatapackSync<GameplayConfigSyncPayload> SYNC =
            new NeoForgeNetwork.DatapackSync<>(
                    GameplayConfigSyncPayload.TYPE,
                    GameplayConfigSyncPayload.STREAM_CODEC,
                    GameplayConfig::accept,
                    server -> new GameplayConfigSyncPayload(changedLines()));

    private GameplayConfig() {}

    private static ModConfigSpec build() {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        Set<String> sections = new HashSet<>();
        mirror(b, sections, ExplosionData.REGISTRY, ExplosionData.groups());
        mirror(b, sections, MobData.REGISTRY, MobData.groups());
        mirror(b, sections, RadiationData.REGISTRY, RadiationData.groups());
        mirror(b, sections, ItemData.REGISTRY, ItemData.groups());
        mirror(b, sections, WorldData.REGISTRY, WorldData.groups());
        {
            // of the machines only the general group: the per-machine numbers stay datapack-only
            List<DataGroups.Group> general =
                    MachineData.groups().stream()
                            .filter(group -> group.id().getPath().equals("machines"))
                            .toList();
            mirror(b, sections, MachineData.REGISTRY, general);
        }
        if (netherAmbientRad == null)
            throw new IllegalStateException(FILE + ": no [radiation] group for nether_ambient_rad");
        return b.build();
    }

    private static void mirror(
            ModConfigSpec.Builder b,
            Set<String> sections,
            ResourceKey<? extends Registry<?>> registry,
            List<DataGroups.Group> groups) {
        for (DataGroups.Group group : groups) {
            String section = group.id().getPath();
            if (!sections.add(section))
                throw new IllegalStateException(FILE + ": two groups named " + section);
            String file =
                    "data/" + group.id().getNamespace() + "/" + registry.location().getNamespace()
                            + "/" + registry.location().getPath() + "/" + section + ".json";
            b.comment(
                            "Same as " + file + " in a datapack.",
                            "A line changed here from its default replaces the datapack's value.")
                    .push(section);
            for (DataGroups.Param<?> param : group.params()) {
                ModConfigSpec.ConfigValue<?> value = define(b, param);
                if (value != null) LINES.add(new Line(section + "." + param.key(), param, value));
            }
            if (section.equals("radiation")) {
                netherAmbientRad =
                        b.comment(
                                        "Background radiation in the Nether, RAD/s (1.7.10:"
                                                + " AMBIENT_00_nether). 0 turns it off.",
                                        "Same as data/minecraft/hbm/radiation_settings/the_nether.json"
                                                + " (ambient_rad). Read when the Nether loads: on a"
                                                + " running server a change takes effect after a"
                                                + " restart.")
                                .defineInRange("nether_ambient_rad", NETHER_AMBIENT_RAD, 0D, 1.0E6D);
            }
            b.pop();
        }
    }

    /**
     * A toml line for one parameter; null for the kinds a toml line can't hold (fuel grade maps).
     * Numbers are plain lines without a toml range: the real limits are the datapack codec's, and
     * checked() applies them (a "Range" comment from defineInRange would show the wrong ones).
     */
    private static ModConfigSpec.ConfigValue<?> define(
            ModConfigSpec.Builder b, DataGroups.Param<?> param) {
        Object def = param.defaultValue();
        if (def instanceof Boolean v) return b.define(param.key(), (boolean) v);
        if (def instanceof Integer || def instanceof Long || def instanceof Double)
            return b.comment("Default: " + def).define(param.key(), def, value -> value instanceof Number);
        if (def instanceof String v) return b.define(param.key(), v);
        return null;
    }

    /** Called by DataGroups.applyAll once the datapack's values are published (server start, remote join). */
    public static void afterDataPack() {
        synchronized (REPORTED) {
            REPORTED.clear();
        }
        synchronized (DATAPACK) {
            for (Line line : LINES) DATAPACK.put(line.param, line.param.get());
        }
        overlay();
    }

    /** The file changed on disk: a running (integrated or dedicated) server takes it and tells the players. */
    public static void onReload() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.execute(
                () -> {
                    // a line that is still (or again) wrong after the edit is reported again
                    synchronized (REPORTED) {
                        REPORTED.clear();
                    }
                    overlay();
                    List<ServerPlayer> players =
                            server.getPlayerList().getPlayers().stream()
                                    .filter(player -> player.connection != null
                                            && player.connection.getConnection() != null)
                                    .filter(player -> !player.connection.getConnection()
                                            .isMemoryConnection())
                                    .toList();
                    if (!players.isEmpty()) SYNC.send(server, players);
                });
    }

    /** Client, joining a remote server: its lines count, not this machine's file. */
    public static void joinedRemote() {
        if (remote == null) remote = Map.of();
    }

    /** Client, leaving a server. */
    public static void left() {
        remote = null;
    }

    private static void accept(GameplayConfigSyncPayload payload) {
        remote = Map.copyOf(payload.lines());
        overlay();
    }

    private static void overlay() {
        Map<String, String> lines = remote;
        synchronized (DATAPACK) {
            for (Line line : LINES) {
                DataGroups.Param<?> param = line.param;
                Object wanted =
                        lines != null ? decode(line, lines.get(line.path)) : checked(line, line.value.get());
                Object value =
                        wanted != null && !wanted.equals(param.defaultValue())
                                ? wanted
                                : DATAPACK.getOrDefault(param, param.defaultValue());
                override(param, value);
            }
        }
    }

    /** The lines of this server's file that differ from their defaults, as JSON for the payload. */
    private static Map<String, String> changedLines() {
        Map<String, String> changed = new LinkedHashMap<>();
        for (Line line : LINES) {
            Object value = checked(line, line.value.get());
            if (value != null && !value.equals(line.param.defaultValue()))
                changed.put(line.path, encode(line.param, value));
        }
        return changed;
    }

    private static Object checked(Line line, Object raw) {
        Object def = line.param.defaultValue();
        // 12.5 in a whole-number line would otherwise be cut to 12 without a word
        if ((def instanceof Integer || def instanceof Long)
                && raw instanceof Number number
                && number.doubleValue() != Math.rint(number.doubleValue()))
            return report(line, DataResult.error(() -> "Expected a whole number: " + raw));
        return report(line, line.param.codec().parse(JavaOps.INSTANCE, raw));
    }

    private static Object decode(Line line, String json) {
        if (json == null || json.isEmpty()) return null;
        return report(line, line.param.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
    }

    private static Object report(Line line, DataResult<?> result) {
        if (result.error().isPresent()) {
            synchronized (REPORTED) {
                if (REPORTED.add(line.path))
                    LOGGER.warn(
                            "{} {}: {}; the datapack's value stays",
                            FILE,
                            line.path,
                            result.error().get().message());
            }
            return null;
        }
        return result.result().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static <T> String encode(DataGroups.Param<T> param, Object value) {
        return param.codec().encodeStart(JsonOps.INSTANCE, (T) value).getOrThrow().toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> void override(DataGroups.Param<T> param, Object value) {
        param.override((T) value);
    }

    /** radiation_settings for the Nether with this file's nether_ambient_rad, if it was changed. */
    public static RadiationSettings netherAmbient(RadiationSettings settings) {
        double rad = netherAmbientRad.get();
        if (rad == NETHER_AMBIENT_RAD) return settings;
        return new RadiationSettings(
                settings.diffusivity(),
                settings.halfLifeSeconds(),
                settings.fogRad(),
                settings.fogChance(),
                settings.worldRadEffects(),
                Optional.of(rad),
                settings.diffusivityTransport());
    }
}
