// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import com.hbm.interfaces.StoredItemSlots;
import com.hbm.interfaces.StoredItems;
import com.hbm.neoforge.hazard.SophisticatedLinkedStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity", remap = false)
public abstract class MixinSophisticatedBackpack implements StoredItems {
    @Shadow private IBackpackWrapper backpackWrapper;

    @Override
    public void visitStoredItems(Visitor visitor) {
        // backport-fix: BF-036 the linked-storage check went through a direct reference to
        // LinkedStorageStackLifecycle (Sophisticated Core 1.5+); with Core 1.4.x the mixin failed to
        // attach and Sophisticated Backpacks crashed the game start. Now behind a presence check.
        if (SophisticatedLinkedStorage.isEndpoint(backpackWrapper.getBackpack())) return;
        visitor.visit((StoredItemSlots) backpackWrapper.getInventoryHandler());
    }
}
