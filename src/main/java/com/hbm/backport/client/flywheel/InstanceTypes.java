// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.hbm.lib.crankshaft.BillboardInstance;
import com.hbm.lib.crankshaft.ClipTransformedInstance;
import com.hbm.lib.crankshaft.CrankShaftInstanceTypes;
import com.hbm.lib.crankshaft.UvTransformedInstance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.instance.PosedInstance;
import dev.engine_room.flywheel.lib.instance.ShadowInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;

/** backport: Flywheel 1.0 InstanceTypes plus the types CrankShaft adds ({@link CrankShaftInstanceTypes}). */
public final class InstanceTypes {
    public static final InstanceType<TransformedInstance> TRANSFORMED = dev.engine_room.flywheel.lib.instance.InstanceTypes.TRANSFORMED;
    public static final InstanceType<PosedInstance> POSED = dev.engine_room.flywheel.lib.instance.InstanceTypes.POSED;
    public static final InstanceType<OrientedInstance> ORIENTED = dev.engine_room.flywheel.lib.instance.InstanceTypes.ORIENTED;
    public static final InstanceType<ShadowInstance> SHADOW = dev.engine_room.flywheel.lib.instance.InstanceTypes.SHADOW;
    public static final InstanceType<UvTransformedInstance> UV_TRANSFORMED = CrankShaftInstanceTypes.UV_TRANSFORMED;
    public static final InstanceType<ClipTransformedInstance> CLIP_TRANSFORMED = CrankShaftInstanceTypes.CLIP_TRANSFORMED;
    public static final InstanceType<BillboardInstance> BILLBOARD = CrankShaftInstanceTypes.BILLBOARD;

    private InstanceTypes() {}
}
