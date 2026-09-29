// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.hbm.lib.crankshaft.CrankShaftMaterials;
import dev.engine_room.flywheel.api.material.LightShader;

/** backport: Flywheel 1.0 LightShaders plus CrankShaft's NONE (no embedded-light lookup). */
public final class LightShaders {
    public static final LightShader NONE = CrankShaftMaterials.LIGHT_NONE;
    public static final LightShader SMOOTH_WHEN_EMBEDDED = dev.engine_room.flywheel.lib.material.LightShaders.SMOOTH_WHEN_EMBEDDED;
    public static final LightShader SMOOTH = dev.engine_room.flywheel.lib.material.LightShaders.SMOOTH;
    public static final LightShader FLAT = dev.engine_room.flywheel.lib.material.LightShaders.FLAT;

    private LightShaders() {}
}
