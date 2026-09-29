// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.hbm.lib.crankshaft.CrankShaftMaterials;
import dev.engine_room.flywheel.api.material.CutoutShader;

/**
 * backport: Flywheel 1.0 CutoutShaders plus CrankShaft's ONE and TINY. CrankShaft's CLIP_SLAB / CLIP_HALFSPACE are
 * not cutouts here: use Materials.CUTOUT_CLIP_SLAB / CUTOUT_CLIP_HALFSPACE (see CrankShaftMaterials).
 */
public final class CutoutShaders {
    public static final CutoutShader OFF = dev.engine_room.flywheel.lib.material.CutoutShaders.OFF;
    public static final CutoutShader EPSILON = dev.engine_room.flywheel.lib.material.CutoutShaders.EPSILON;
    public static final CutoutShader ONE_TENTH = dev.engine_room.flywheel.lib.material.CutoutShaders.ONE_TENTH;
    public static final CutoutShader HALF = dev.engine_room.flywheel.lib.material.CutoutShaders.HALF;
    public static final CutoutShader ONE = CrankShaftMaterials.CUTOUT_ONE;
    public static final CutoutShader TINY = CrankShaftMaterials.CUTOUT_TINY;

    private CutoutShaders() {}
}
