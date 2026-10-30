// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.compat.createitem;

import com.google.common.base.Predicate;
import com.hbm.integration.create.CreateMultiblockCells;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport-fix: BF-118 a Create funnel (and anything else on Create's inventory manipulation: brass
 * funnels, deployers' and smart observers' inventories) on a physics build reaches an NTM multiblock
 * cell. Create asks the block in front for its capability; Sable replaces the block-entity lookup
 * there with a search through the build, the world and other builds that only stops at a block
 * entity, so for a cell (no block entity) it ran on and settled on the empty world point under the
 * build. The core and single blocks, which have one, worked; in the world everything did. For an NTM
 * cell the capability is asked right there, as Create itself does without Sable and as a vanilla
 * hopper does. Applied only with Create installed (ClientCompatPlugin).
 */
@Mixin(value = CapManipulationBehaviourBase.class, remap = false)
public abstract class CreateCapManipulationCellMixin {

    @Shadow protected Object targetCapability;
    @Shadow protected boolean bypassSided;
    @Shadow protected Predicate<BlockEntity> filter;

    @Shadow
    public abstract BlockFace getTarget();

    @Shadow
    protected abstract BlockCapability<?, Direction> capability();

    @Inject(method = "findNewCapability", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbm$multiblockCell(CallbackInfo ci) {
        Level level = ((BlockEntityBehaviour) (Object) this).getWorld();
        if (level == null) return;
        BlockFace face = getTarget().getOpposite();
        BlockPos cell = face.getPos();
        if (!CreateMultiblockCells.isNtmCell(level, cell)) return;
        targetCapability = null;
        if (filter.apply(null) && level.isLoaded(cell))
            targetCapability =
                    level.getCapability(capability(), cell, bypassSided ? null : face.getFace());
        ci.cancel();
    }
}
