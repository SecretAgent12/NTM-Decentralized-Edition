// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.ISectionGeneration;
import com.hbm.blocks.ClimbBoxes;
import com.hbm.interfaces.injected.IClimbBoxSection;
import com.hbm.util.SectionGeneration;
import com.hbm.util.SectionMetadata;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunkSection.class)
public abstract class MixinLevelChunkSection implements IClimbBoxSection , ISectionGeneration {
    @Unique public long hbm$generation;
    @Unique private short hbm$climbBoxes;

    @Override
    public int hbm$climbBoxes() {
        return hbm$climbBoxes;
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/world/level/chunk/PalettedContainer;Lnet/minecraft/world/level/chunk/PalettedContainerRO;)V",
            at = @At("RETURN"))
    private void hbm$countLoaded(CallbackInfo ci) {
        hbm$recountClimbBoxes();
    }

    // backport: 26.x also copies the climb-box count / section metadata in the copy constructor
    // and copy(); 1.21.1 LevelChunkSection has neither.

    @ModifyReturnValue(
            method =
                    "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"))
    private BlockState hbm$blockChanged(
            BlockState previous, int x, int y, int z, BlockState state, boolean checked) {
        if (previous != state) {
            SectionGeneration.changed((LevelChunkSection) (Object) this);
            if (ClimbBoxes.counts(previous)) hbm$climbBoxes--;
            if (ClimbBoxes.counts(state)) hbm$climbBoxes++;
        }
        return previous;
    }

    @Inject(
            method = {"read", "readBiomes", "fillBiomesFromNoise"},
            at = @At("RETURN"))
    private void hbm$paletteChanged(CallbackInfo ci) {
        SectionGeneration.changed((LevelChunkSection) (Object) this);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void hbm$countRead(CallbackInfo ci) {
        hbm$recountClimbBoxes();
    }

    @Unique
    private void hbm$recountClimbBoxes() {
        PalettedContainer<BlockState> states = ((LevelChunkSection) (Object) this).getStates();
        if (!states.maybeHas(ClimbBoxes::counts)) {
            hbm$climbBoxes = 0;
            return;
        }
        int[] total = {0};
        states.count(
                (state, count) -> {
                    if (ClimbBoxes.counts(state)) total[0] += count;
                });
        hbm$climbBoxes = (short) total[0];
    }

    // backport: accessors replacing ntm-next's raw bytecode field access
    @Override
    public long hbm$generation() {
        return hbm$generation;
    }

    @Override
    public void hbm$setGeneration(long value) {
        hbm$generation = value;
    }
}
