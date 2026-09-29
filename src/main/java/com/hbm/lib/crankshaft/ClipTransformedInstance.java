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
 * {@link TransformedInstance} + slide vec3 + clip plane vec4 (OBJ-local, tested against {@code Position + slide}); the
 * seed's zero plane is accepted everywhere. backport: the clip test runs in the material fragment shader of
 * {@link CrankShaftMaterials#CUTOUT_CLIP_SLAB} / {@link CrankShaftMaterials#CUTOUT_CLIP_HALFSPACE} (Flywheel 1.0 has no
 * backend clip varying), so those materials still require this instance type.
 */
public class ClipTransformedInstance extends TransformedInstance implements Slab.Holder {
    static final int OFF_SLIDE = 76;
    static final int OFF_PLANE = 88;
    private final Slab slab;

    public ClipTransformedInstance(InstanceType<? extends ClipTransformedInstance> type, InstanceHandle handle) {
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

    public ClipTransformedInstance setSlide(float x, float y, float z) {
        ExtraMemoryOps.putVector3f(slabPtr() + OFF_SLIDE, x, y, z);
        return this;
    }

    public ClipTransformedInstance setPlane(float nx, float ny, float nz, float threshold) {
        ExtraMemoryOps.putVector4f(slabPtr() + OFF_PLANE, nx, ny, nz, threshold);
        return this;
    }
}
