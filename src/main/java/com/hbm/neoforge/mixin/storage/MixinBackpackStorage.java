// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.neoforge.inventory.SophisticatedContentsStorage;
import com.hbm.neoforge.inventory.SophisticatedItemInteriors;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// backport: Sophisticated 1.21.1 stores each record as a CompoundTag (Map<UUID, CompoundTag>)
@Mixin(value = BackpackStorage.class, remap = false)
public abstract class MixinBackpackStorage implements SophisticatedContentsStorage {
    @Shadow @Final private Map<UUID, CompoundTag> backpackContents;

    @Override
    public @Nullable CompoundTag storedContents(UUID id) {
        return backpackContents.get(id);
    }

    @Inject(method = "getOrCreateBackpackContents", at = @At("HEAD"), cancellable = true)
    private void hbm$existingContents(UUID id, CallbackInfoReturnable<CompoundTag> ci) {
        CompoundTag current = backpackContents.get(id);
        if (current != null) ci.setReturnValue(current);
    }

    @Inject(method = "getOrCreateBackpackContents", at = @At("RETURN"))
    private void hbm$createdContents(UUID id, CallbackInfoReturnable<CompoundTag> ci) {
        SophisticatedItemInteriors.backpackRecordChanged(id);
    }

    @Inject(method = "setBackpackContents", at = @At("RETURN"))
    private void hbm$recordReplaced(UUID id, CompoundTag contents, CallbackInfo ci) {
        SophisticatedItemInteriors.backpackRecordChanged(id);
    }

    @Inject(method = "removeBackpackContents", at = @At("RETURN"))
    private void hbm$recordRemoved(UUID id, CallbackInfo ci) {
        SophisticatedItemInteriors.backpackRecordChanged(id);
    }
}
