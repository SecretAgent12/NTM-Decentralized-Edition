// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.ponder;

import com.hbm.lib.Library;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** backport: NTM's Ponder scenes. Schematics: assets/hbm/ponder/*.nbt, texts: hbm.ponder.* keys. */
public final class NTMPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return "hbm";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation firebox = Library.id("heater_firebox");
        ResourceLocation boiler = Library.id("machine_boiler");
        ResourceLocation furnace = Library.id("furnace_steel");

        helper.forComponents(firebox, boiler)
                .addStoryBoard("firebox_boiler", NTMPonderScenes::fireboxBoiler);
        helper.forComponents(firebox, furnace)
                .addStoryBoard("firebox_furnace", NTMPonderScenes::fireboxFurnace);
    }
}
