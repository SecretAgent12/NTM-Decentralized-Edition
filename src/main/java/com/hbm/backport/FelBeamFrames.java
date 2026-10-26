// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.blockentity.machine.BlockEntitySILEX;
import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

/**
 * Sable compat (not in ntm-next or 1.7.10): the FEL beam between the world and physics builds
 * (Sable sub-levels). The FEL steps its beam block by block in its own space - the plot, if it
 * stands on a build. A cell that is air there is looked up where it is drawn in the world: the
 * world block at that point (a FEL on a build shooting at the ground), then the blocks of any
 * build drawn there (any FEL shooting at a build). The beam then burns that block, or feeds the
 * SILEX it passes through.
 *
 * <p>The SILEX: in one space the FEL finds the core by a fixed offset from the cell the beam enters
 * (one on, one down). Across spaces the cell the beam lands in depends on where the build floats, a
 * fraction of a block off the grid, so instead the SILEX is found by its block entity around the
 * hit ({@link #silexCore}) and fed when the beam runs along its laser axis - the row above the
 * core, along its facing - within {@link #ALIGN_DEG} and {@link #AXIS_REACH} blocks ({@link
 * #meet}). Every SILEX takes the beam once per shot: the beam passes through the rest of its
 * cells (the same SILEX found again is not a second, badly spaced one). As in one space, a beam
 * into its side burns it; only the few degrees a build swings by are let off.
 *
 * <p>{@link #of} returns null when the FEL stands in the world and no build is near its beam:
 * then the FEL runs exactly as before. Goes through Sable Companion (bundled, a no-op without
 * Sable).
 */
public final class FelBeamFrames {

    /** How far off (degrees) a SILEX on another build / in the world may be and still take the beam. */
    public static final double ALIGN_DEG = 5.0;

    private static final double ALIGN_COS = Math.cos(Math.toRadians(ALIGN_DEG));

    /** How far (blocks) the beam may run beside a SILEX's laser axis and still feed it. */
    public static final double AXIS_REACH = 0x1.8p-1;

    /**
     * Degrees off the SILEX axis from which the beam is taken as hitting it from the wrong side
     * (walls, pipes) and burns it, as a wrongly turned SILEX burns in one space. Between {@link
     * #ALIGN_DEG} and this a build is only swinging: the beam is stopped, nothing burns.
     */
    public static final double WRONG_SIDE_DEG = 45.061;

    private static final double WRONG_SIDE_COS = Math.cos(Math.toRadians(WRONG_SIDE_DEG));

    /** How a beam from another space meets a SILEX. */
    public enum SilexHit {
        /** Along the laser axis: the SILEX takes the beam. */
        FEEDS,
        /** Along the axis direction but beside the window: the beam passes, as in one space. */
        PASSES,
        /** A little off (a swinging build): the beam stops, nothing burns. */
        BLOCKED,
        /** From the side (walls, pipes): the SILEX burns, as in one space. */
        WRONG_SIDE
    }

    /** How far around a hit SILEX cell its core is looked for (the SILEX is 3 wide, 4 long). */
    private static final int CORE_SEARCH = 3;

    /** A block the beam reached in another space: a world position or another build's plot position. */
    public record Hit(BlockPos pos, BlockState state) {}

    private final @Nullable SubLevelAccess own;
    private final SubLevelBlast builds;
    private final Vec3 worldDir;
    private final Vec3 worldStart;

    private FelBeamFrames(@Nullable SubLevelAccess own, SubLevelBlast builds, Vec3 worldStart, Vec3 worldDir) {
        this.own = own;
        this.builds = builds;
        this.worldStart = worldStart;
        this.worldDir = worldDir;
    }

    /** The frames of a FEL at {@code fel} firing along {@code dir} up to {@code range}, or null if there is nothing to do. */
    public static @Nullable FelBeamFrames of(Level level, BlockPos fel, Direction dir, int range) {
        SubLevelAccess own = SableCompanion.INSTANCE.getContaining(level, fel);
        Vec3 start = Vec3.atCenterOf(fel.above());
        Vec3 end = start.add(dir.getStepX() * range, 0, dir.getStepZ() * range);
        Vec3 ws = toWorld(own, start), we = toWorld(own, end);
        Vec3 mid = ws.add(we).scale(0.5);
        SubLevelBlast builds = SubLevelBlast.around(level, mid.x, mid.y, mid.z, range / 2.0 + 2);
        if (own == null && builds.isEmpty()) return null;
        Vec3 d = we.subtract(ws);
        return new FelBeamFrames(own, builds, ws, d.lengthSqr() < 1e-9 ? Vec3.ZERO : d.normalize());
    }

    /** True when the FEL itself stands on a build. */
    public boolean onBuild() {
        return own != null;
    }

    /** What is drawn in the world where the FEL's own (empty) cell {@code beam} is, or null. */
    public @Nullable Hit at(Level level, BlockPos beam) {
        Vec3 p = toWorld(own, Vec3.atCenterOf(beam));
        if (own != null) {
            BlockPos world = BlockPos.containing(p);
            BlockState state = level.getBlockState(world);
            if (!state.isAir()) return new Hit(world, state);
        }
        if (builds.isEmpty()) return null;
        BlockPos plot = builds.plotBlock(level, p.x, p.y, p.z);
        return plot == null ? null : new Hit(plot.immutable(), level.getBlockState(plot));
    }

    /**
     * The core of the SILEX a beam hit in another space, or null: the cell itself, the multiblock
     * index, else the nearest SILEX block entity around it (the index of a build's plot may not know
     * the core yet).
     */
    public static @Nullable BlockPos silexCore(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof BlockEntitySILEX) return pos;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos, state);
        if (core != null && level.getBlockEntity(core) instanceof BlockEntitySILEX) return core;
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -CORE_SEARCH; dx <= CORE_SEARCH; dx++)
            for (int dy = -2; dy <= 1; dy++)
                for (int dz = -CORE_SEARCH; dz <= CORE_SEARCH; dz++) {
                    at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!(level.getBlockEntity(at) instanceof BlockEntitySILEX)) continue;
                    int d = dx * dx + dy * dy + dz * dz;
                    if (d < bestDist) {
                        bestDist = d;
                        best = at.immutable();
                    }
                }
        return best;
    }

    /**
     * How the beam meets the SILEX at {@code core} facing {@code facing} (both in the SILEX's own
     * space). It feeds it when it runs along the laser axis - through the row above the core, along
     * the facing either way - within {@link #ALIGN_DEG} and {@link #AXIS_REACH}; see {@link SilexHit}
     * for the rest.
     */
    public SilexHit meet(Level level, BlockPos core, Direction facing) {
        SubLevelAccess at = SableCompanion.INSTANCE.getContaining(level, core);
        Vec3 axisPoint = Vec3.atCenterOf(core.above());
        Vector3d f = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        if (at != null) {
            axisPoint = at.logicalPose().transformPosition(axisPoint);
            f = at.logicalPose().transformNormal(f);
        }
        double cos = Math.abs(f.x * worldDir.x + f.y * worldDir.y + f.z * worldDir.z) / f.length();
        if (cos < WRONG_SIDE_COS) return SilexHit.WRONG_SIDE;
        if (cos < ALIGN_COS) return SilexHit.BLOCKED;
        // distance from the axis point to the beam line
        Vec3 off = axisPoint.subtract(worldStart);
        return off.cross(worldDir).length() <= AXIS_REACH ? SilexHit.FEEDS : SilexHit.PASSES;
    }

    /**
     * The living entities along the beam drawn in the world, from the FEL's cell {@code from} to
     * {@code to} (both in the FEL's space): a FEL on a build looks for them where the beam is drawn.
     */
    public List<LivingEntity> entitiesAlong(Level level, Vec3 from, Vec3 to) {
        Vec3 a = toWorld(own, from), b = toWorld(own, to);
        AABB box = new AABB(a, b).inflate(0.6);
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        return level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> {
                    Vec3 c = e.getBoundingBox().getCenter();
                    double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, c.subtract(a).dot(ab) / len2));
                    Vec3 near = a.add(ab.scale(t));
                    return e.getBoundingBox().inflate(0.3).contains(near);
                });
    }

    private static Vec3 toWorld(@Nullable SubLevelAccess sub, Vec3 p) {
        return sub == null ? p : sub.logicalPose().transformPosition(p);
    }
}
