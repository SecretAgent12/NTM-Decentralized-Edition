// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.interfaces.RigidPistonStructure;
import com.hbm.interfaces.StoredItems;
import com.hbm.interfaces.injected.MovedBlockEntityData;
import com.hbm.items.special.CarriedBlockItems;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import com.hbm.backport.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.storage.TagValueInput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.hbm.backport.BlockEntityCompat;

@Mixin(PistonMovingBlockEntity.class)
public abstract class MixinPistonMovingBlockEntity implements MovedBlockEntityData, StoredItems {

    @Unique private static final Logger HBM$LOGGER = LoggerFactory.getLogger("NTM");

    @Unique private static final String HBM$CARRIED = "hbmMovedData";

    @Unique private @Nullable CompoundTag hbm$carried;

    @Unique private @Nullable BlockEntity hbm$carriedInventory;

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (hbm$carried == null || !(hbm$self().getLevel() instanceof ServerLevel server)) return;
        if (hbm$carriedInventory == null) {
            hbm$carriedInventory =
                    CarriedBlockItems.load(
                            getMovedState(),
                            hbm$self().getBlockPos(),
                            hbm$carried,
                            server.registryAccess());
        }
        if (hbm$carriedInventory != null && visitor.visit(hbm$carriedInventory)) {
            hbm$carried = hbm$carriedInventory.saveCustomOnly(server.registryAccess());
        }
    }

    @WrapMethod(method = "tick")
    private static void hbm$tickRigidly(
            Level level,
            BlockPos pos,
            BlockState state,
            PistonMovingBlockEntity entity,
            Operation<Void> original) {
        boolean rigid = entity.getMovedState().getBlock() instanceof RigidPistonStructure;
        boolean previous = BlockMultiblockCore.isBusy();
        if (rigid) BlockMultiblockCore.setBusy(true);
        try {
            original.call(level, pos, state, entity);
        } finally {
            if (rigid) BlockMultiblockCore.setBusy(previous);
        }
        if (rigid) entity.hbm$restoreCarried();
    }

    @Shadow
    public abstract BlockState getMovedState();

    @Unique
    private PistonMovingBlockEntity hbm$self() {
        return (PistonMovingBlockEntity) (Object) this;
    }

    @Override
    public void hbm$captureFrom(BlockEntity source) {
        Level level = source.getLevel();
        if (level == null) return;
        this.hbm$carried = source.saveCustomOnly(level.registryAccess());
        this.hbm$carriedInventory = null;
    }

    @Override
    public void hbm$restoreCarried() {
        if (this.hbm$carried == null) return;
        PistonMovingBlockEntity self = hbm$self();
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = self.getBlockPos();

        if (!level.getBlockState(pos).is(getMovedState().getBlock())) return;
        BlockEntity landed = level.getBlockEntity(pos);
        if (landed == null) return;

        CompoundTag data = this.hbm$carried;
        this.hbm$carried = null;
        this.hbm$carriedInventory = null;
        try (ProblemReporter.ScopedCollector reporter =
                new ProblemReporter.ScopedCollector(landed.toString(), HBM$LOGGER)) {
            BlockEntityCompat.loadCustomOnly(landed, TagValueInput.tagOf(TagValueInput.create(reporter, level.registryAccess(), data)));
        }
        landed.setChanged();
    }

    /**
     * backport: 26.x runs finalTick from PistonMovingBlockEntity.preRemoveSideEffects and spills the
     * carried contents there when they could not be restored. In 1.21.1 the equivalent removal path
     * is MovingPistonBlock.onRemove -> finalTick(), called after the chunk already holds the new
     * state; so a finalTick that starts with the moving piston no longer in place is a removal,
     * and the 26.x spill runs after it.
     */
    @WrapMethod(method = "finalTick")
    private void hbm$landRigidly(Operation<Void> original) {
        PistonMovingBlockEntity self = hbm$self();
        Level level = self.getLevel();
        boolean removed =
                level != null
                        && !level.getBlockState(self.getBlockPos()).is(Blocks.MOVING_PISTON);
        boolean rigid = getMovedState().getBlock() instanceof RigidPistonStructure;
        boolean previous = BlockMultiblockCore.isBusy();
        if (rigid) BlockMultiblockCore.setBusy(true);
        try {
            original.call();
        } finally {
            if (rigid) BlockMultiblockCore.setBusy(previous);
        }
        if (rigid) hbm$restoreCarried();
        if (removed) hbm$spillIfLost(self.getBlockPos());
    }

    @Unique
    private void hbm$spillIfLost(BlockPos pos) {
        if (this.hbm$carried == null) return;

        CompoundTag data = this.hbm$carried;
        this.hbm$carried = null;
        this.hbm$carriedInventory = null;
        BlockState moved = getMovedState();
        if (!(hbm$self().getLevel() instanceof ServerLevel server)) return;
        if (!(moved.getBlock() instanceof EntityBlock entityBlock)) return;
        BlockEntity ghost = entityBlock.newBlockEntity(pos, moved);
        if (ghost == null) return;

        ghost.setLevel(server);
        try (ProblemReporter.ScopedCollector reporter =
                new ProblemReporter.ScopedCollector(ghost.toString(), HBM$LOGGER)) {
            BlockEntityCompat.loadCustomOnly(ghost, TagValueInput.tagOf(TagValueInput.create(reporter, server.registryAccess(), data)));
        }
        if (ghost instanceof BlockEntityMachineBase machine) machine.spillInventory(pos);
        else if (ghost instanceof Container container)
            Containers.dropContents(server, pos, container);
    }

    // backport: 1.21.1 saveAdditional/loadAdditional(CompoundTag, HolderLookup.Provider)
    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void hbm$saveCarried(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (this.hbm$carried != null) tag.put(HBM$CARRIED, this.hbm$carried.copy());
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void hbm$loadCarried(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.hbm$carried =
                tag.contains(HBM$CARRIED, Tag.TAG_COMPOUND) ? tag.getCompound(HBM$CARRIED) : null;
        this.hbm$carriedInventory = null;
    }
}
