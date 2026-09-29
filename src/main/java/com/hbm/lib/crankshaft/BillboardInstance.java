// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import com.hbm.backport.client.flywheel.ExtraMemoryOps;
import com.hbm.backport.client.flywheel.Slab;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import org.lwjgl.system.MemoryUtil;

/**
 * A camera-facing sprite anchored at {@code position}: the vertex shader orients the mesh toward the camera plane.
 * backport: position/size/uvRegion (and any subclass fields) live in a {@link Slab}; colour, light and overlay stay
 * Flywheel 1.0 fields written over it.
 */
public class BillboardInstance extends ColoredLitOverlayInstance implements Slab.Holder {
    static final int OFF_POSITION = 12;
    static final int OFF_SIZE = 24;
    static final int OFF_UV_REGION = 28;
    private final Slab slab;

    public BillboardInstance(InstanceType<? extends BillboardInstance> type, InstanceHandle handle) {
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

    public BillboardInstance position(float x, float y, float z) {
        ExtraMemoryOps.putVector3f(slabPtr() + OFF_POSITION, x, y, z);
        return this;
    }

    public BillboardInstance size(float size) {
        MemoryUtil.memPutFloat(slabPtr() + OFF_SIZE, size);
        return this;
    }

    public BillboardInstance uvRegion(float offU, float offV, float scaleU, float scaleV) {
        ExtraMemoryOps.putVector4f(slabPtr() + OFF_UV_REGION, offU, offV, scaleU, scaleV);
        return this;
    }
}
