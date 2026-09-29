/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer.access;

import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.item.PlayerInventoryWrapper;
import com.hbm.lib.neotransfer.transaction.TransactionContext;

class PlayerItemAccess implements ItemAccess {
    private final PlayerInventoryWrapper inventoryWrapper;
    private final ResourceHandler<ItemResource> slot;

    public PlayerItemAccess(PlayerInventoryWrapper inventoryWrapper, ResourceHandler<ItemResource> slot) {
        this.inventoryWrapper = inventoryWrapper;
        this.slot = slot;
    }

    @Override
    public ItemResource getResource() {
        return slot.getResource(0);
    }

    @Override
    public int getAmount() {
        return slot.getAmountAsInt(0);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        int inserted = slot.insert(0, resource, amount, transaction);
        if (amount > inserted) {
            inventoryWrapper.placeItemBackInInventory(resource, amount - inserted, transaction);
        }
        // Any leftover is dropped, so the full amount can always be accepted
        return amount;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        return slot.extract(0, resource, amount, transaction);
    }
}
