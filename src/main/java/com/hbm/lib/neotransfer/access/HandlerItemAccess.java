/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer.access;

import java.util.Objects;
import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.TransactionContext;

public class HandlerItemAccess implements ItemAccess {
    protected final ResourceHandler<ItemResource> handler;
    protected final int index;

    public HandlerItemAccess(ResourceHandler<ItemResource> handler, int index) {
        Objects.checkIndex(index, handler.size());
        this.handler = handler;
        this.index = index;
    }

    @Override
    public ItemResource getResource() {
        return handler.getResource(index);
    }

    @Override
    public int getAmount() {
        return handler.getAmountAsInt(index);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        int inserted = handler.insert(index, resource, amount, transaction);
        if (inserted < amount) {
            // Insert any leftover into the rest of the handler
            inserted += handler.insert(resource, amount - inserted, transaction);
        }
        return inserted;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        return handler.extract(index, resource, amount, transaction);
    }
}
