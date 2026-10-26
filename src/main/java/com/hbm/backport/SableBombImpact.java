// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.blockentity.bomb.BlockEntityBombMulti;
import com.hbm.blockentity.bomb.BlockEntityNukeAssembly;
import com.hbm.blockentity.bomb.BlockEntityNukeBalefire;
import com.hbm.blockentity.bomb.BlockEntityNukeCustom;
import com.hbm.blocks.bomb.BombMulti;
import com.hbm.blocks.bomb.BombRotatableBlock;
import com.hbm.blocks.bomb.NukeBalefire;
import com.hbm.blocks.bomb.NukeCustom;
import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.interfaces.IBomb;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

/**
 * Sable compat (not in ntm-next or 1.7.10): an armed bomb with a nose and tail (Little Boy, Fat Man,
 * Tsar Bomba, custom nuke, Solinium, balefire bomb, multi purpose bomb) goes off when it hits
 * something nose first, like a contact fuze. Hooked up by MixinSableBombImpact, i.e. only with
 * Sable installed.
 *
 * <ul>
 *   <li>Armed: the same check that decides a redstone signal - the assembly is ready (all
 *       components in), the custom nuke has any charge, the balefire bomb has its egg and battery,
 *       the multi purpose bomb is loaded. An unarmed bomb is just a heavy block.
 *   <li>Nose ({@link #nose}), checked against the OBJ meshes (the tapered end is the nose, the
 *       finned end the tail) and how the blockstates / models turn them: the nukes on {@link
 *       BombRotatableBlock} point their FACING meta (2 north, 3 south, 4 west, 5 east), the
 *       multi purpose bomb its FACING, the balefire bomb FACING turned clockwise. The contact point
 *       Sable reports is in the block's own (plot) space, so the face it lies on is the one whose
 *       axis dominates the offset from the block centre.
 *   <li>Speed: {@link #TRIGGER_VELOCITY}, about a 5 block free fall - lowering a bomb onto a deck
 *       or a gentle docking doesn't set it off.
 * </ul>
 *
 * <p>The callback runs inside the physics step, so the bomb is only marked here and detonated
 * through its own {@link IBomb#explode} on the next server task (the same path as redstone); a
 * blast entity spawned inside the plot is moved to the world by Sable, and MK5 then reaches the
 * builds around it (BF-050).
 */
public final class SableBombImpact implements BlockSubLevelCollisionCallback {

    public static final SableBombImpact INSTANCE = new SableBombImpact();

    /** m/s (blocks per second) of the contact needed to fire. */
    public static final double TRIGGER_VELOCITY = 10.0103;

    /** Minimal offset of the contact from the centre along the nose, i.e. on or near the nose face. */
    private static final double NOSE_DEPTH = 0.35;

    private static final Set<Long> PENDING = new HashSet<>();

    private SableBombImpact() {}

    @Override
    public CollisionResult sable$onCollision(
            BlockPos pos, @Nullable BlockPos otherHitBlockPos, Vector3d impactPosition, double impactVelocity) {
        if (impactVelocity * impactVelocity < TRIGGER_VELOCITY * TRIGGER_VELOCITY) return CollisionResult.NONE;

        ServerLevel level = SubLevelPhysicsSystem.getCurrentlySteppingSystem().getLevel();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IBomb bomb)) return CollisionResult.NONE;
        Direction nose = nose(state);
        if (nose == null || !noseHit(nose, pos, impactPosition) || !armed(level, pos, state))
            return CollisionResult.NONE;

        BlockPos at = pos.immutable();
        synchronized (PENDING) {
            if (!PENDING.add(at.asLong())) return new CollisionResult(new Vector3d(), true);
        }
        MinecraftServer server = level.getServer();
        server.tell(
                new TickTask(
                        server.getTickCount(),
                        () -> {
                            synchronized (PENDING) {
                                PENDING.remove(at.asLong());
                            }
                            if (level.getBlockState(at).getBlock() == state.getBlock())
                                bomb.explode(level, at, null);
                        }));
        return new CollisionResult(new Vector3d(), true);
    }

    /** Which way the bomb's nose points, or null for a block without one. */
    static @Nullable Direction nose(BlockState state) {
        if (state.getBlock() instanceof BombRotatableBlock)
            return Direction.from3DDataValue(state.getValue(BombRotatableBlock.FACING));
        if (state.getBlock() instanceof NukeBalefire)
            return state.getValue(BlockMachineHorizontal.FACING).getClockWise();
        if (state.getBlock() instanceof BombMulti) return state.getValue(BlockMachineHorizontal.FACING);
        return null;
    }

    /** Whether the contact lies on the nose face of the bomb at {@code pos}. */
    static boolean noseHit(Direction nose, BlockPos pos, Vector3d contact) {
        double dx = contact.x - (pos.getX() + 0.5);
        double dy = contact.y - (pos.getY() + 0.5);
        double dz = contact.z - (pos.getZ() + 0.5);
        double along = dx * nose.getStepX() + dy * nose.getStepY() + dz * nose.getStepZ();
        if (along < NOSE_DEPTH) return false;
        // the nose axis has to dominate: a contact on the edge between nose and side counts, a side
        // contact that is merely forward of centre doesn't
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        return along >= Math.max(ax, Math.max(ay, az)) - 2.610302E-6;
    }

    /** The redstone condition: would {@link IBomb#explode} actually detonate it. */
    static boolean armed(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof NukeCustom) {
            if (!(level.getBlockEntity(pos) instanceof BlockEntityNukeCustom custom)) return false;
            BlockEntityNukeCustom.Totals t = custom.totals();
            return t.tnt() > 0
                    || t.nuke() > 0
                    || t.hydro() > 0
                    || t.amat() > 0
                    || t.dirty() > 0
                    || t.schrab() > 0
                    || t.euph() > 0;
        }
        if (state.getBlock() instanceof NukeBalefire)
            return level.getBlockEntity(pos) instanceof BlockEntityNukeBalefire bf && bf.isLoaded();
        if (state.getBlock() instanceof BombMulti)
            return level.getBlockEntity(pos) instanceof BlockEntityBombMulti multi && multi.isLoaded();
        return level.getBlockEntity(pos) instanceof BlockEntityNukeAssembly assembly && assembly.isReady();
    }
}
