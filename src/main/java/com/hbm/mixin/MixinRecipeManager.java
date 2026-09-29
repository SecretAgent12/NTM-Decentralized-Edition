// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.google.gson.JsonElement;
import com.hbm.inventory.recipes.DatapackRecipeLoad;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public abstract class MixinRecipeManager {

    @Shadow @Final private HolderLookup.Provider registries;

    /**
     * backport: 26.x RecipeManager.apply(RecipeMap, ...) receives the parsed recipes; 1.21.1's
     * apply(Map&lt;ResourceLocation, JsonElement&gt;, ...) parses them itself into byName, so the
     * datapack rows are bound from getRecipes() once apply has finished.
     */
    @Inject(
            method =
                    "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL"))
    private void hbm$bindDatapackRows(
            Map<ResourceLocation, JsonElement> json,
            ResourceManager manager,
            ProfilerFiller profiler,
            CallbackInfo ci) {
        DatapackRecipeLoad.apply(((RecipeManager) (Object) this).getRecipes(), registries);
    }
}
