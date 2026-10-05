// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockFallout;
import com.hbm.config.FalloutConfigJSON;
import com.hbm.config.FalloutConfigJSON.FalloutEntry;
import com.hbm.explosion.ExplosionNukeRayBatched;
import com.hbm.util.ChunkUtil;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import it.unimi.dsi.fastutil.longs.LongArrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * backport-fix: BF-050 — a nuclear blast (MK5) also hits the physics builds (Sable sub-levels) in
 * its reach. Its ray engine runs on worker threads over world chunks, and a build's blocks live in a
 * far-away plot, so the builds came out of the blast untouched. Instead of reaching into that engine,
 * each build gets one pass of its own on the server thread, sized by the build, not by the blast:
 *
 * <ul>
 *   <li>{@link #crater}: the MK5 ray rule, cell by cell inside the build. Rays start at the blast
 *       (moved into the build's plot); each cell inherits what's left of the ray from the cell one
 *       step back towards the blast and pays the same cost the world rays pay. Cells left with
 *       energy are removed. The world between the blast and the build isn't counted (it's open air
 *       where builds fly), and plot distances are taken as world distances (builds aren't scaled).
 *   <li>{@link #fallout}: what the fallout rain does to a world column (fallout layers, fires,
 *       the fallout table's block changes, three solid blocks deep), applied to the build's
 *       columns. Run when the rain is spawned, so it only happens when the blast has fallout.
 * </ul>
 */
public final class SubLevelNuke {

    // the same numbers as the world engines (ExplosionNukeRayBatched / -Parallelized)
    private static final float NUKE_RESISTANCE_CUTOFF = 2_000_000F;
    private static final float INITIAL_ENERGY_FACTOR = 0.3F;
    // and as EntityFalloutRain
    private static final int MAX_SOLID_DEPTH = 3;
    // a build bigger than this (plot cells of its bounds) is skipped rather than stalling the tick
    private static final long MAX_CELLS = 2_000_000L;

    private SubLevelNuke() {}

    /** Removes what an MK5 blast of {@code strength} / {@code radius} at {@code (x, y, z)} blows away. */
    public static void crater(
            ServerLevel level, double x, double y, double z, int strength, int radius) {
        Vec3 blast = SubLevelBlast.origin(level, x, y, z);
        for (SubLevelAccess sub : intersecting(level, blast, radius)) {
            Box box = Box.of(sub);
            if (box == null) continue;
            Vec3 o = sub.logicalPose().transformPositionInverse(blast);
            craterIn(level, sub.getUniqueId(), box, o, strength, radius);
        }
    }

    private static void craterIn(
            ServerLevel level, UUID id, Box box, Vec3 o, int strength, int radius) {
        int sx = box.sizeX(), sy = box.sizeY(), sz = box.sizeZ();
        int cells = sx * sy * sz;

        // cells within the radius, nearest first: a cell's step back towards the blast is always
        // nearer, so it's done by the time the cell itself is
        long[] order = new long[cells];
        int n = 0;
        for (int i = 0; i < cells; i++) {
            double d = box.center(i).distanceTo(o);
            if (d > radius) continue;
            order[n++] = ((long) Float.floatToIntBits((float) d) << 32) | i;
        }
        LongArrays.radixSort(order, 0, n);

        float[] left = new float[cells];
        boolean[] done = new boolean[cells];
        float start = strength * INITIAL_ENERGY_FACTOR;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Owner owner = new Owner(level, id);
        ArrayList<BlockPos> doomed = new ArrayList<>();

        for (int k = 0; k < n; k++) {
            int i = (int) order[k];
            float d = Float.intBitsToFloat((int) (order[k] >>> 32));
            Vec3 c = box.center(i);

            // what's left of the ray arriving here: from one step back towards the blast
            float energy = start;
            if (d > 1F) {
                Vec3 back = c.add(o.subtract(c).scale(1D / d));
                int b = box.index(Mth.floor(back.x), Mth.floor(back.y), Mth.floor(back.z));
                if (b >= 0 && done[b]) energy = left[b];
            }

            box.pos(i, pos);
            BlockState state = owner.owns(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
            if (state.getBlock().getExplosionResistance() >= NUKE_RESISTANCE_CUTOFF) {
                energy = 0F;
            } else {
                double r0 = Math.max(d, 1D);
                energy -=
                        (float)
                                (Math.pow(
                                                ExplosionNukeRayBatched.getNukeResistance(state)
                                                        + 1,
                                                3.0D * r0 / radius)
                                        - 1.0D);
                if (energy > 0F && !state.isAir()) doomed.add(pos.immutable());
            }
            left[i] = energy;
            done[i] = true;
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos p : doomed) {
            BlockState old = level.getBlockState(p);
            level.setBlock(p, air, Block.UPDATE_CLIENTS);
            ChunkUtil.dispatchRemovalHook(level, p, old, air);
        }
    }

    /** What the fallout rain of {@code scale} at {@code (x, y, z)} does, applied to the builds. */
    public static void fallout(ServerLevel level, double x, double y, double z, int scale) {
        if (scale <= 0) return;
        Vec3 blast = SubLevelBlast.origin(level, x, y, z);
        List<FalloutEntry> table = FalloutConfigJSON.entries(level.registryAccess());
        RandomSource random = level.getRandom();
        Block fallout = ModBlocks.FALLOUT.get();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Vector3d scratch = new Vector3d();

        for (SubLevelAccess sub : intersecting(level, blast, scale)) {
            Box box = Box.of(sub);
            if (box == null) continue;
            Pose3dc pose = sub.logicalPose();
            Owner owner = new Owner(level, sub.getUniqueId());

            for (int bx = box.minX; bx <= box.maxX; bx++) {
                for (int bz = box.minZ; bz <= box.maxZ; bz++) {
                    pos.set(bx, box.maxY, bz);
                    if (!owner.owns(pos)) continue;
                    int solidDepth = 0;

                    for (int by = box.maxY; by >= box.minY; by--) {
                        if (solidDepth >= MAX_SOLID_DEPTH) break;
                        BlockState state = level.getBlockState(pos.set(bx, by, bz));
                        if (state.isAir() || state.is(fallout)) continue;

                        pose.transformPosition(scratch.set(bx + 0.5D, by + 0.5D, bz + 0.5D), scratch);
                        double dx = scratch.x - blast.x, dz = scratch.z - blast.z;
                        double percent = Math.sqrt(dx * dx + dz * dz) * 100.0D / scale;
                        if (percent > 100.0D) continue;

                        BlockState above = level.getBlockState(pos.set(bx, by + 1, bz));
                        if (solidDepth == 0
                                && (above.isAir() || (above.canBeReplaced() && !above.liquid()))) {
                            double d = percent / 100.0D;
                            double chance = 0.1D - Math.pow(d - 0.7D, 2);
                            if (chance >= random.nextDouble()) {
                                BlockState layer = null;
                                if (above.is(fallout)) {
                                    int existing = above.getValue(BlockFallout.LAYERS);
                                    if (existing < SnowLayerBlock.MAX_HEIGHT)
                                        layer = above.setValue(BlockFallout.LAYERS, existing + 1);
                                } else {
                                    layer = fallout.defaultBlockState();
                                }
                                if (layer != null) {
                                    level.setBlock(pos, layer, Block.UPDATE_CLIENTS);
                                    above = layer;
                                }
                            }
                        }

                        if (percent < 65D
                                && ((FireBlock) Blocks.FIRE).getBurnOdds(state) > 0
                                && above.isAir()
                                && random.nextInt(5) == 0) {
                            level.setBlock(
                                    pos.set(bx, by + 1, bz),
                                    Blocks.FIRE.defaultBlockState(),
                                    Block.UPDATE_ALL);
                        }

                        boolean transformed = false;
                        pos.set(bx, by, bz);
                        for (FalloutEntry entry : table) {
                            BlockState result = entry.eval(level, pos, state, percent, random);
                            if (result != null) {
                                level.setBlock(pos, result, Block.UPDATE_CLIENTS);
                                if (entry.restrictDepth()) solidDepth++;
                                transformed = true;
                                break;
                            }
                        }
                        if (!transformed && state.canOcclude()) solidDepth++;
                    }
                }
            }
        }
    }

    private static List<SubLevelAccess> intersecting(ServerLevel level, Vec3 at, double reach) {
        BoundingBox3d box =
                new BoundingBox3d(
                        at.x - reach, at.y - reach, at.z - reach,
                        at.x + reach, at.y + reach, at.z + reach);
        ArrayList<SubLevelAccess> found = new ArrayList<>();
        for (SubLevelAccess sub : SableCompanion.INSTANCE.getAllIntersecting(level, box))
            found.add(sub);
        return found;
    }

    /** Only the cells of this build's own plot (its bounds, turned back into the plot, overreach). */
    private static final class Owner {
        private final ServerLevel level;
        private final UUID id;
        private int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
        private boolean owned;

        Owner(ServerLevel level, UUID id) {
            this.level = level;
            this.id = id;
        }

        boolean owns(BlockPos pos) {
            int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
            if (cx != chunkX || cz != chunkZ) {
                chunkX = cx;
                chunkZ = cz;
                SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(level, cx, cz);
                owned = sub != null && id.equals(sub.getUniqueId());
            }
            return owned;
        }
    }

    /** The plot cells under a build's world bounds. */
    private record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        static Box of(SubLevelAccess sub) {
            BoundingBox3dc world = sub.boundingBox();
            Pose3dc pose = sub.logicalPose();
            double[] xs = {world.minX(), world.maxX()};
            double[] ys = {world.minY(), world.maxY()};
            double[] zs = {world.minZ(), world.maxZ()};
            double lx = Double.MAX_VALUE, ly = Double.MAX_VALUE, lz = Double.MAX_VALUE;
            double hx = -Double.MAX_VALUE, hy = -Double.MAX_VALUE, hz = -Double.MAX_VALUE;
            Vector3d p = new Vector3d();
            for (double x : xs)
                for (double y : ys)
                    for (double z : zs) {
                        pose.transformPositionInverse(p.set(x, y, z), p);
                        lx = Math.min(lx, p.x);
                        ly = Math.min(ly, p.y);
                        lz = Math.min(lz, p.z);
                        hx = Math.max(hx, p.x);
                        hy = Math.max(hy, p.y);
                        hz = Math.max(hz, p.z);
                    }
            Box box =
                    new Box(
                            Mth.floor(lx), Mth.floor(ly), Mth.floor(lz),
                            Mth.floor(hx), Mth.floor(hy), Mth.floor(hz));
            long cells = (long) box.sizeX() * box.sizeY() * box.sizeZ();
            return cells <= 0 || cells > MAX_CELLS ? null : box;
        }

        int sizeX() {
            return maxX - minX + 1;
        }

        int sizeY() {
            return maxY - minY + 1;
        }

        int sizeZ() {
            return maxZ - minZ + 1;
        }

        int index(int x, int y, int z) {
            if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) return -1;
            return ((x - minX) * sizeY() + (y - minY)) * sizeZ() + (z - minZ);
        }

        void pos(int i, BlockPos.MutableBlockPos out) {
            int z = i % sizeZ();
            int rest = i / sizeZ();
            int y = rest % sizeY();
            int x = rest / sizeY();
            out.set(minX + x, minY + y, minZ + z);
        }

        Vec3 center(int i) {
            int z = i % sizeZ();
            int rest = i / sizeZ();
            int y = rest % sizeY();
            int x = rest / sizeY();
            return new Vec3(minX + x + 0.5D, minY + y + 0.5D, minZ + z + 0.5D);
        }
    }
}
