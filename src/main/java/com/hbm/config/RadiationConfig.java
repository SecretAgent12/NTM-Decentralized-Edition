// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import com.hbm.data.RadiationData;

public final class RadiationConfig {

    public static int hazardRate = 5;

    public static int radTickRate = 1;

    public static boolean enableDebugMode = false;

    // backport-fix: BF-126 config line for coal dust (1.7.10 HAZ_01_disableCoaldust)
    public static boolean disableCoalDust = false;

    private RadiationConfig() {}

    public static void loadFrom(ConfigStore c) {
        enableDebugMode = c.get(ConfigSchema.ENABLE_DEBUG_MODE);
        radTickRate = c.get(ConfigSchema.RAD_TICK_RATE);
        hazardRate = c.get(ConfigSchema.HAZARD_RATE);
        disableCoalDust = c.get(ConfigSchema.DISABLE_COAL_DUST);
    }

    /** backport-fix: BF-126 coal dust is off by the config line or by the hazards datapack value. */
    public static boolean coalDustDisabled() {
        return disableCoalDust || RadiationData.DISABLE_COAL.get();
    }
}
