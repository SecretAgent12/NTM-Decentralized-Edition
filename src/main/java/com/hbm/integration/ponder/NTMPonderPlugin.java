// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.ponder;

import com.hbm.lib.Library;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

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
        // backport: Create electric motors, only with Create (their blocks exist only then)
        if (ModList.get().isLoaded("create"))
            com.hbm.integration.create.motor.CreateMotorScenes.registerScenes(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        if (ModList.get().isLoaded("create"))
            com.hbm.integration.create.motor.CreateMotorScenes.registerTags(helper);
    }
}
