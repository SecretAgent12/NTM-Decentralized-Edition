// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.IInventorySources;
import com.hbm.capability.ForeignBridges;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.injected.ForeignBridgeHost;
import com.hbm.inventory.BlockItemContents;
import com.hbm.inventory.InventoryChanges;
import com.hbm.items.ModDataComponents;
import net.minecraft.core.BlockPos;
import com.hbm.backport.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import com.hbm.backport.storage.TagValueOutput;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class MixinBlockEntity implements ForeignBridgeHost, IInventorySources {
    @Unique public Object hbm$inventorySources;
    @Unique private @Nullable ForeignBridges hbm$bridges;

    // backport: accessors replacing ntm-next's raw bytecode field access (InventoryChanges.sources)
    @Override
    public Object hbm$inventorySources() {
        return hbm$inventorySources;
    }

    @Override
    public void hbm$setInventorySources(Object sources) {
        hbm$inventorySources = sources;
    }

    @Override
    public ForeignBridges hbm$bridges() {
        ForeignBridges bridges = hbm$bridges;
        if (bridges == null) hbm$bridges = bridges = new ForeignBridges();
        return bridges;
    }

    @Inject(method = "collectImplicitComponents", at = @At("TAIL"))
    private void hbm$collectPackedInventory(DataComponentMap.Builder components, CallbackInfo ci) {
        BlockItemContents contents = BlockItemContents.capture((BlockEntity) (Object) this);
        if (contents != null) components.set(ModDataComponents.BLOCK_INVENTORY.get(), contents);
    }

    // backport: 1.21.1 removeComponentsFromTag takes the CompoundTag (26.x: ValueOutput); DE's
    // BlockEntityCompat routes its ValueOutput form back here through super
    @Inject(method = "removeComponentsFromTag", at = @At("TAIL"))
    private void hbm$stripPackedInventory(CompoundTag tag, CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        BlockItemContents.strip(
                self,
                TagValueOutput.wrap(
                        tag, level != null ? level.registryAccess() : RegistryAccess.EMPTY));
    }

    // backport: 26.x applyImplicitComponents(DataComponentGetter) TAIL. In 1.21.1 it takes a
    // DataComponentInput, and DE's BlockEntityCompat overrides it without calling the vanilla one,
    // so the restore follows the call in applyComponents instead (every block entity); the read
    // goes through the same input, so the packed inventory is consumed like other implicit
    // components
    @WrapOperation(
            method = "applyComponents",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/entity/BlockEntity;applyImplicitComponents(Lnet/minecraft/world/level/block/entity/BlockEntity$DataComponentInput;)V"))
    private void hbm$restorePackedInventory(
            BlockEntity self, BlockEntity.DataComponentInput input, Operation<Void> original) {
        original.call(self, input);
        BlockItemContents.apply(
                self,
                new DataComponentGetter() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T get(DataComponentType<? extends T> type) {
                        return (T) input.get(type);
                    }
                });
    }

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void hbm$retire(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel level)) return;
        self.getBlockState().getBlock().hbm$blockEntityRemoved(level, self);
        if (UnstableFuses.isStorageOwner(self)) UnstableFuses.removed(level, self);
    }

    @Inject(
            method =
                    "setChanged(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("TAIL"))
    private static void hbm$storageChanged(
            Level world, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!(world instanceof ServerLevel level)) return;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;
        BlockEntity self = chunk.getBlockEntities().get(pos);
        if (self == null) return;
        if (UnstableFuses.isStorageOwner(self)) InventoryChanges.blockChanged(self);
    }
}
