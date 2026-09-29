// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import com.hbm.backport.client.flywheel.ExtraMemoryOps;
import com.hbm.backport.client.flywheel.Slab;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;

/**
 * {@link TransformedInstance} + a per-instance atlas UV-region; the vertex shader remaps the mesh UV into the sub-rect.
 * backport: the extra fields live in a {@link Slab} (CrankShaft writes them straight into the GPU slot); colour, light,
 * overlay and pose stay Flywheel 1.0 fields, written over the slab by {@link CrankShaftInstanceTypes}' writers.
 */
public class UvTransformedInstance extends TransformedInstance implements Slab.Holder {
    static final int OFF_UV_REGION = 76;
    private final Slab slab;

    public UvTransformedInstance(InstanceType<? extends UvTransformedInstance> type, InstanceHandle handle) {
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

    public UvTransformedInstance uvRegion(float offU, float offV, float scaleU, float scaleV) {
        ExtraMemoryOps.putVector4f(slabPtr() + OFF_UV_REGION, offU, offV, scaleU, scaleV);
        return this;
    }
}
