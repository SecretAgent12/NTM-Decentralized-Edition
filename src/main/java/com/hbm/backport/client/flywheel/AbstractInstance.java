// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;

/**
 * backport: CrankShaft's AbstractInstance adds {@code slabPtr()} (the instance's slot address). Here the slot is a
 * per-instance {@link Slab}, copied into Flywheel 1.0's buffer by {@link SimpleInstanceType}'s default writer.
 */
public abstract class AbstractInstance extends dev.engine_room.flywheel.lib.instance.AbstractInstance
        implements Slab.Holder {
    private final Slab slab;

    protected AbstractInstance(InstanceType<?> type, InstanceHandle handle) {
        super(type, handle);
        slab = new Slab(type);
    }

    @Override
    public final Slab slab() {
        return slab;
    }

    protected final long slabPtr() {
        return slab.address;
    }
}
