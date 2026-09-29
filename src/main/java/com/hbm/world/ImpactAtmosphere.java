// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.data.WorldData;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.platform.Services;
import com.hbm.saveddata.TomSaveData;
import net.minecraft.server.level.ServerLevel;
import com.hbm.backport.ARGB;
import net.minecraft.world.level.Level;

public final class ImpactAtmosphere {

    private ImpactAtmosphere() {}

    // backport: 26.x layers these onto its environment attribute system (sky light, star
    // brightness, sky/cloud/fog/sunrise colours; MixinEnvironmentAttributeSystem). 1.21.1 has no
    // such system, so the same adjustments are plain functions for the 1.21.1 hooks to call
    // (sky darkening, the fog-colour event and the sky/cloud colour mixins): same formulas.
    public static boolean active(Level level) {
        return level.dimension() == Level.OVERWORLD && WorldData.ENABLE_IMPACT_ATMOSPHERE.get();
    }

    /** SKY_LIGHT_LEVEL layer: pulled towards the night value by the dust. */
    public static float skyLightLevel(Level level, float value, float nightValue) {
        return active(level) ? value - (value - nightValue) * dust(level) : value;
    }

    /** SKY_LIGHT_FACTOR and STAR_BRIGHTNESS layers. */
    public static float dimmed(Level level, float value) {
        return active(level) ? value * (1 - dust(level)) : value;
    }

    /** SUNRISE_SUNSET_COLOR layer. */
    public static int sunriseColor(Level level, int value) {
        if (!active(level)) return value;
        float clear = 1 - dust(level);
        return ARGB.scaleRGB(ARGB.multiplyAlpha(value, clear), clear);
    }

    /** CLOUD_COLOR layer. */
    public static int cloudColor(Level level, int value) {
        return active(level) ? ARGB.scaleRGB(value, 1 - dust(level)) : value;
    }

    /** SKY_COLOR layer. */
    public static int skyColor(Level level, int value) {
        return active(level) ? skyColor(value, dust(level), fire(level)) : value;
    }

    /** FOG_COLOR layer. */
    public static int fogColor(Level level, int value) {
        return active(level) ? fogColor(value, dust(level), fire(level)) : value;
    }

    public static int skyColor(int sky, float dust, float fire) {
        float brightness = fire + (1 - dust);
        if (fire > 0) {
            return ARGB.scaleRGB(
                    sky,
                    1.3F * brightness,
                    Math.max(1 - dust * 1.4F, 0) * brightness,
                    Math.max(1 - dust * 4, 0) * brightness);
        }
        return ARGB.scaleRGB(
                sky, brightness, (1 - dust * 0.5F) * brightness, (1 - dust) * brightness);
    }

    public static int fogColor(int fog, float dust, float fire) {
        float brightness = fire > 0 ? Math.max(1 - dust * 2, 0) : 1 - dust;
        return ARGB.scaleRGB(
                fog, brightness, (1 - dust * 0.5F) * brightness, (1 - dust) * brightness);
    }

    private static float dust(Level level) {
        if (level instanceof ServerLevel server) {
            TomSaveData data = TomSaveData.published(server);
            return data == null ? 0 : data.dust;
        }
        return ImpactWorldHandler.getDustForClient(level);
    }

    private static float fire(Level level) {
        if (level instanceof ServerLevel server) {
            TomSaveData data = TomSaveData.published(server);
            return data == null ? 0 : data.fire;
        }
        return ImpactWorldHandler.getFireForClient(level);
    }
}
