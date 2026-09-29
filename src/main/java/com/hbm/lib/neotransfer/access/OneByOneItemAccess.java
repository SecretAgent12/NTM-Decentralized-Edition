/*
 * Copyright (c) NeoForged and contributors
 * Modified by SecretAgent12 (NTM 1.21.1 backport): ported to Minecraft 1.21.1, package relocated.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.hbm.lib.neotransfer.access;

import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.TransactionContext;

record OneByOneItemAccess(ItemAccess delegate) implements ItemAccess {
    @Override
    public ItemResource getResource() {
        return delegate.getResource();
    }

    @Override
    public int getAmount() {
        return Math.min(1, delegate.getAmount());
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        return delegate.insert(resource, Math.min(amount, 1), transaction);
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        return delegate.extract(resource, Math.min(amount, 1), transaction);
    }
}
