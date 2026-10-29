// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.blockentity.network.BlockEntityPipeAnchor;
import com.hbm.blockentity.network.BlockEntityPylonBase;
import com.hbm.blocks.network.FluidPipeAnchorBlock;
import com.hbm.platform.NeoForgeServerEvents;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.ChunkUtil;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sable compat (not in ntm-next): a wire or pipe between a physics build and the world (or two
 * builds) snaps when the build moves its ends further apart than the wire may be long. Links
 * between the same space can't stretch and are never watched.
 *
 * <p>Pylons register their crossing links whenever their connections are refreshed, pipe anchors on
 * their tick ({@link #track}). Every {@link #PERIOD} ticks each watched link with both ends loaded
 * is measured where it is drawn; past its length (the shorter pylon's reach, or the pipe anchor
 * limit) plus {@link #SLACK} it is cut from its graph ({@link LevelNodeGraph#cutRemoteLink}), both
 * ends redraw, and it snaps audibly at both ends. An end that isn't loaded keeps the link as it is
 * until both are back.
 */
public final class SubLevelLinkWatch {

    public enum Kind {
        POWER,
        FLUID
    }

    private record Link(long a, long b, Kind kind) {
        static Link of(long x, long y, Kind kind) {
            return x < y ? new Link(x, y, kind) : new Link(y, x, kind);
        }
    }

    /** Ticks between two checks. */
    public static final int PERIOD = 0xA;

    /** Blocks a link may be stretched past its length before it snaps (sway of a moving build). */
    public static final double SLACK = 1.0103;

    private static final Map<ServerLevel, Set<Link>> WATCHED = new WeakHashMap<>();
    private static int clock;

    private SubLevelLinkWatch() {}

    /** Hooked up once at start (from {@link SubLevelChunks#init}). */
    public static void init() {
        NeoForgeServerEvents.onServerTickPost(SubLevelLinkWatch::tick);
        NeoForgeServerEvents.onServerStopping(server -> WATCHED.clear());
    }

    /** Watches the link {@code a}-{@code b} if it crosses spaces; cheap for links that don't. */
    public static void track(ServerLevel level, long a, long b, Kind kind) {
        if (!SubLevelLinks.crosses(level, BlockPos.of(a), BlockPos.of(b))) return;
        WATCHED.computeIfAbsent(level, l -> new HashSet<>()).add(Link.of(a, b, kind));
    }

    private static void tick(MinecraftServer server) {
        if (++clock % PERIOD != 0 || WATCHED.isEmpty()) return;
        for (Map.Entry<ServerLevel, Set<Link>> entry : WATCHED.entrySet()) {
            ServerLevel level = entry.getKey();
            for (Iterator<Link> it = entry.getValue().iterator(); it.hasNext(); ) {
                Link link = it.next();
                Boolean keep = check(level, link);
                if (keep != null && !keep) it.remove();
            }
        }
    }

    /** False: the link is gone (cut now or earlier) and no longer watched; true or null: keep watching. */
    private static @Nullable Boolean check(ServerLevel level, Link link) {
        BlockPos a = BlockPos.of(link.a()), b = BlockPos.of(link.b());
        return switch (link.kind()) {
            case POWER -> {
                BlockEntityPylonBase pa = ChunkUtil.blockEntityIfLoaded(BlockEntityPylonBase.class, level, a);
                BlockEntityPylonBase pb = ChunkUtil.blockEntityIfLoaded(BlockEntityPylonBase.class, level, b);
                if (pa == null || pb == null) yield null;
                LevelNodeGraph<?> graph = PowerGraph.get(level);
                if (!linked(graph, link)) yield false;
                double reach = Math.min(pa.maxWireLength(), pb.maxWireLength());
                if (SubLevelLinks.worldDistance(level, pa.connectionPoint(), pb.connectionPoint()) <= reach + SLACK)
                    yield true;
                graph.cutRemoteLink(link.a(), link.b());
                pa.refreshConnections();
                pb.refreshConnections();
                snap(level, pa.connectionPoint(), pb.connectionPoint());
                yield false;
            }
            case FLUID -> {
                BlockEntityPipeAnchor fa = ChunkUtil.blockEntityIfLoaded(BlockEntityPipeAnchor.class, level, a);
                BlockEntityPipeAnchor fb = ChunkUtil.blockEntityIfLoaded(BlockEntityPipeAnchor.class, level, b);
                if (fa == null || fb == null) yield null;
                LevelNodeGraph<?> graph = FluidPipeGraph.graphAt(level, link.a());
                if (graph == null || !linked(graph, link)) yield false;
                Vec3 ca = Vec3.atCenterOf(a), cb = Vec3.atCenterOf(b);
                if (SubLevelLinks.worldDistance(level, ca, cb) <= FluidPipeAnchorBlock.MAX_PIPE_LENGTH + SLACK)
                    yield true;
                graph.cutRemoteLink(link.a(), link.b());
                snap(level, ca, cb);
                yield false; // the anchors pick up their new connections on their next tick
            }
        };
    }

    private static boolean linked(LevelNodeGraph<?> graph, Link link) {
        GraphNode<?> node = graph.getNode(link.a());
        return node != null && node.hasRemoteLinks() && node.remoteLinks.contains(link.b());
    }

    private static void snap(ServerLevel level, Vec3 a, Vec3 b) {
        for (Vec3 p : new Vec3[] {SubLevelLinks.toWorld(level, a), SubLevelLinks.toWorld(level, b)}) {
            level.playSound(null, p.x, p.y, p.z, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1F, 0.8F);
        }
    }
}
