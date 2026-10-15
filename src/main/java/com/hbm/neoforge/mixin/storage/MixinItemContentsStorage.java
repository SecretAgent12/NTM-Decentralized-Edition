// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.neoforge.inventory.SophisticatedContentsStorage;
import com.hbm.neoforge.inventory.SophisticatedItemInteriors;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// backport: Sophisticated 1.21.1 stores each record as a CompoundTag (Map<UUID, CompoundTag>)
// and names the record methods get/set/removeStorageContents
@Mixin(value = ItemContentsStorage.class, remap = false)
public abstract class MixinItemContentsStorage implements SophisticatedContentsStorage {
    @Shadow @Final private Map<UUID, CompoundTag> storageContents;

    @Override
    public @Nullable CompoundTag storedContents(UUID id) {
        return storageContents.get(id);
    }

    @Inject(method = "getOrCreateStorageContents", at = @At("HEAD"), cancellable = true)
    private void hbm$existingContents(UUID id, CallbackInfoReturnable<CompoundTag> ci) {
        CompoundTag current = storageContents.get(id);
        if (current != null) ci.setReturnValue(current);
    }

    @Inject(method = "getOrCreateStorageContents", at = @At("RETURN"))
    private void hbm$createdContents(UUID id, CallbackInfoReturnable<CompoundTag> ci) {
        SophisticatedItemInteriors.storageRecordChanged(id);
    }

    @Inject(method = "setStorageContents", at = @At("RETURN"))
    private void hbm$recordReplaced(UUID id, CompoundTag contents, CallbackInfo ci) {
        SophisticatedItemInteriors.storageRecordChanged(id);
    }

    @Inject(method = "removeStorageContents", at = @At("RETURN"))
    private void hbm$recordRemoved(UUID id, CallbackInfo ci) {
        SophisticatedItemInteriors.storageRecordChanged(id);
    }
}
