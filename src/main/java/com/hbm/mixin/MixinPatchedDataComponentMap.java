// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.IInventorySlotBinding;
import com.hbm.backport.IItemInteriorCache;
import com.hbm.backport.IUnstableDeadline;
import com.hbm.inventory.InventorySlotBinding;
import com.hbm.inventory.ItemContents;
import com.hbm.packet.SyncSource;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.hbm.packet.SyncBindings;

@Mixin(PatchedDataComponentMap.class)
public abstract class MixinPatchedDataComponentMap implements SyncSource , IUnstableDeadline, IInventorySlotBinding, IItemInteriorCache {
    @Unique public Object hbm$inventorySource;
    @Unique public int hbm$inventorySlot;
    @Unique public Object[] hbm$interiorReferences;
    @Unique public boolean hbm$opaqueReferences;
    @Unique public long hbm$unstableDeadline = Long.MIN_VALUE;

    @Inject(
            method =
                    "set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;",
            at = @At("HEAD"))
    private void hbm$invalidateFuseSet(
            DataComponentType<?> type, Object value, CallbackInfoReturnable<?> ci) {
        if (!ItemContents.affectsMetadata(type)) return;
        hbm$unstableDeadline = Long.MIN_VALUE;
        hbm$interiorReferences = null;
    }

    @Inject(
            method = "remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("HEAD"))
    private void hbm$invalidateFuseRemove(DataComponentType<?> type, CallbackInfoReturnable<?> ci) {
        if (!ItemContents.affectsMetadata(type)) return;
        hbm$unstableDeadline = Long.MIN_VALUE;
        hbm$interiorReferences = null;
    }

    @Inject(
            method = {
                "applyPatch(Lnet/minecraft/core/component/DataComponentPatch;)V",
                "restorePatch"
                // backport: 26.x also hooks clearPatch(), which 1.21.1 does not have
            },
            at = @At("HEAD"))
    private void hbm$invalidateFusePatch(CallbackInfo ci) {
        hbm$unstableDeadline = Long.MIN_VALUE;
        hbm$interiorReferences = null;
        syncChanged(3);
    }

    @Inject(
            method = {
                "applyPatch(Lnet/minecraft/core/component/DataComponentPatch;)V",
                "restorePatch"
                // backport: 26.x also hooks clearPatch(), which 1.21.1 does not have
            },
            at = @At("RETURN"))
    private void hbm$publishFusePatch(CallbackInfo ci) {
        InventorySlotBinding.changed((PatchedDataComponentMap) (Object) this);
    }

    @Inject(
            method =
                    "set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;",
            at = @At("RETURN"))
    private <T> void hbm$syncValue(
            DataComponentType<T> type, T next, CallbackInfoReturnable<T> ci) {
        if (syncBound() && ci.getReturnValue() != next) syncChanged(3);
        if (ci.getReturnValue() != next)
            InventorySlotBinding.changed((PatchedDataComponentMap) (Object) this);
    }

    @Inject(
            method = "remove(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("RETURN"))
    private void hbm$syncRemove(DataComponentType<?> type, CallbackInfoReturnable<?> ci) {
        if (ci.getReturnValue() != null) syncChanged(3);
        if (ci.getReturnValue() != null)
            InventorySlotBinding.changed((PatchedDataComponentMap) (Object) this);
    }


    // backport: woven trait SyncSource
    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }

    // backport: accessors replacing ntm-next's raw bytecode field access
    @Override
    public long hbm$unstableDeadline() {
        return hbm$unstableDeadline;
    }

    @Override
    public void hbm$setUnstableDeadline(long value) {
        hbm$unstableDeadline = value;
    }

    @Override
    public Object hbm$inventorySource() {
        return hbm$inventorySource;
    }

    @Override
    public void hbm$setInventorySource(Object source) {
        hbm$inventorySource = source;
    }

    @Override
    public int hbm$inventorySlot() {
        return hbm$inventorySlot;
    }

    @Override
    public void hbm$setInventorySlot(int slot) {
        hbm$inventorySlot = slot;
    }

    @Override
    public Object[] hbm$interiorReferences() {
        return hbm$interiorReferences;
    }

    @Override
    public void hbm$setInteriorReferences(Object[] references) {
        hbm$interiorReferences = references;
    }

    @Override
    public boolean hbm$opaqueReferences() {
        return hbm$opaqueReferences;
    }

    @Override
    public void hbm$setOpaqueReferences(boolean opaque) {
        hbm$opaqueReferences = opaque;
    }
}
