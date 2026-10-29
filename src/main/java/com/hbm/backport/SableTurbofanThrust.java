// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.blockentity.machine.BlockEntityMachineTurbofan;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Vector3d;

/**
 * Sable compat (not in ntm-next or 1.7.10): a running turbofan on a physics build pushes the build.
 * Called each physics step by MixinSableTurbofanThrust (only with Sable installed).
 *
 * <p>The engine blows air out of its back (the entities behind it are shoved that way, see the
 * turbofan's blast), so the build is pushed the other way, towards the intake, through the
 * machine's core (an engine off the centre of mass turns the build, as it should). The force is
 * {@link BlockEntityMachineTurbofan#currentThrust}: full thrust for the afterburner level times the
 * throttle (classic control: full or nothing; throttle control: redstone signal / 15). With Sable's
 * default mass of 1 per block, the base thrust lifts about 200 blocks against gravity.
 *
 * <p>Applied the way Sable's own propellers do it (BlockEntitySubLevelPropellerActor): recorded
 * into the build's PROPULSION force group, so it shows in the contraption diagram and adds up with
 * the propellers.
 */
public final class SableTurbofanThrust {

    private SableTurbofanThrust() {}

    public static void physicsTick(BlockEntityMachineTurbofan fan, ServerSubLevel sub, double dt) {
        if (!fan.wasOn || dt <= 0 || sub == null) return;
        double force = fan.currentThrust();
        if (!(force > 2.610301E-9)) return;
        Direction towards = BlockMultiblockCore.coreFacing(fan.getBlockState()).getClockWise();
        // the build's own (plot) frame: block directions are the build's directions; force x step,
        // as the propellers pass it
        {
            BlockPos core = fan.getBlockPos();
            sub.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get())
                    .applyAndRecordPointForce(
                            new Vector3d(core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5),
                            new Vector3d(towards.getStepX(), towards.getStepY(), towards.getStepZ())
                                    .mul(dt * force));
        }
    }
}
