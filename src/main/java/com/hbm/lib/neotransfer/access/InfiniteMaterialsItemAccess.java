/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer.access;

import net.minecraft.world.entity.player.Player;
import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.ResourceHandlerUtil;
import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.item.PlayerInventoryWrapper;
import com.hbm.lib.neotransfer.transaction.TransactionContext;

class InfiniteMaterialsItemAccess implements ItemAccess {
    private final ResourceHandler<ItemResource> mainSlots;
    private final ItemResource resource;
    private final int amount;

    InfiniteMaterialsItemAccess(Player player, ItemResource resource, int amount) {
        this.mainSlots = PlayerInventoryWrapper.of(player).getMainSlots();
        this.resource = resource;
        this.amount = amount;
    }

    @Override
    public ItemResource getResource() {
        return resource;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount > 0 && !ResourceHandlerUtil.contains(mainSlots, resource)) {
            // Only add the item to the player inventory if it's not already in the inventory.
            mainSlots.insert(resource, 1, transaction);
        }

        // Insertion always succeeds from the POV of the access' user.
        return amount;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        // Extraction always succeeds from the POV of the access' user.
        return amount;
    }
}
