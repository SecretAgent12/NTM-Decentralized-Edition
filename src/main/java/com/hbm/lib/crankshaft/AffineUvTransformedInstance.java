// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import org.lwjgl.system.MemoryUtil;

/**
 * Affine pose with a full two-dimensional UV transform. Both loaders use the same layout; owning visual
 * tasks mutate instances before the render barrier. Geometry buffers stay immutable as triangles deform.
 */
public final class AffineUvTransformedInstance extends UvTransformedInstance {
    static final int OFF_UV_SHEAR = 92;

    public static final InstanceType<AffineUvTransformedInstance> TYPE = CrankShaftInstanceTypes.AFFINE_UV_TRANSFORMED;

    public AffineUvTransformedInstance(InstanceType<? extends AffineUvTransformedInstance> type,
                                       InstanceHandle handle) {
        super(type, handle);
    }

    public AffineUvTransformedInstance uv(float m00, float m01, float m10, float m11, float offsetU, float offsetV) {
        super.uvRegion(offsetU, offsetV, m00, m11);
        MemoryUtil.memPutFloat(slabPtr() + OFF_UV_SHEAR, m01);
        MemoryUtil.memPutFloat(slabPtr() + OFF_UV_SHEAR + 4, m10);
        return this;
    }

    @Override
    public AffineUvTransformedInstance uvRegion(float offsetU, float offsetV, float scaleU, float scaleV) {
        return uv(scaleU, 0, 0, scaleV, offsetU, offsetV);
    }
}
