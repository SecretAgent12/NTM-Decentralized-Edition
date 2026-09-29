// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.misc;

import com.hbm.client.SootFog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 26.x applies {@link SootFog} to the FogData returned by FogRenderer.setupFog (MixinFogRenderer).
 * 1.21.1 has no FogData: fog distances come from ViewportEvent.RenderFog and the fog colour
 * from ViewportEvent.ComputeFogColor, so the same SootFog.apply runs on a FogData built from
 * each event and the result is written back.
 *
 * backport: 26.x keeps environmental fog (soot) and render-distance fog separate and combines
 * them in the shader; 1.21.1 has a single linear terrain fog, so soot replaces the terrain fog
 * range (start 0, end shortened) — soot's end is never beyond the render distance, so the
 * effect is the same except the edge fog band is not layered on top. Sky fog (FOG_SKY) is left
 * alone, as 26.x soot does not touch skyEnd.
 */
public final class SootFogEvents {

    private SootFogEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SootFogEvents::onRenderFog);
        NeoForge.EVENT_BUS.addListener(SootFogEvents::onFogColor);
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        boolean atmospheric = event.getType() == FogType.NONE;
        if (!atmospheric) return; // only the colour changes under fluids/powder snow
        FogData fog = new FogData();
        fog.environmentalStart = fog.renderDistanceStart = event.getNearPlaneDistance();
        fog.environmentalEnd = fog.renderDistanceEnd = event.getFarPlaneDistance();
        SootFog.apply(fog, true, Minecraft.getInstance().options.getEffectiveRenderDistance());
        if (fog.environmentalStart == event.getNearPlaneDistance()
                && fog.environmentalEnd == event.getFarPlaneDistance()) return;
        event.setNearPlaneDistance(fog.environmentalStart);
        event.setFarPlaneDistance(fog.environmentalEnd); // 26.x overwrites the environment fog (also blindness) the same way
        event.setCanceled(true); // NeoForge applies the new distances only when cancelled
    }

    private static void onFogColor(ViewportEvent.ComputeFogColor event) {
        FogData fog = new FogData();
        fog.color.set(event.getRed(), event.getGreen(), event.getBlue(), 1F);
        // atmospheric=false: only the colour part of SootFog.apply runs here
        SootFog.apply(fog, false, Minecraft.getInstance().options.getEffectiveRenderDistance());
        event.setRed(fog.color.x);
        event.setGreen(fog.color.y);
        event.setBlue(fog.color.z);
    }
}
