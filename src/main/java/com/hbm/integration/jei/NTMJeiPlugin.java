// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jei;

import com.hbm.client.gui.ScreenAnvil;
import com.hbm.client.gui.ScreenMachineAutocrafter;
import com.hbm.client.gui.ScreenMachineCustom;
import com.hbm.integration.recipeviewer.CraftingRows;
import com.hbm.integration.recipeviewer.CustomMachinePage;
import com.hbm.integration.recipeviewer.RecipePage;
import com.hbm.integration.recipeviewer.RecipePages;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@JeiPlugin
public class NTMJeiPlugin implements IModPlugin {

    private final Map<ResourceKey<CustomMachineDefinition>, CustomMachinePage> customMachines =
            new LinkedHashMap<>();
    private final Map<ResourceLocation, JeiLookupPlugin<?>> lookupPlugins = new HashMap<>();

    // backport: JEI 19.x RecipeType (26.x JEI IRecipeType.create(id, class)); same uid and row class
    private static <T> RecipeType<T> typeOf(RecipePage<T> page) {
        return new RecipeType<>(page.id(), page.rowType());
    }

    private List<RecipePage<?>> pages() {
        List<RecipePage<?>> pages = new ArrayList<>(RecipePages.fixed());
        pages.addAll(customMachines.values());
        return pages;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return Library.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helpers = registration.getJeiHelpers();
        long unitsPerMilliBucket = helpers.getPlatformFluidHelper().bucketVolume() / 1000L;
        customMachines.clear();
        for (CustomMachinePage page : RecipePages.customMachines()) {
            customMachines.put(page.key(), page);
        }
        lookupPlugins.clear();
        for (RecipePage<?> page : pages())
            register(registration, page, helpers, unitsPerMilliBucket);
    }

    private <T> void register(
            IRecipeCategoryRegistration registration,
            RecipePage<T> page,
            IJeiHelpers helpers,
            long unitsPerMilliBucket) {
        JeiPageCategory<T> category =
                new JeiPageCategory<>(
                        page, typeOf(page), helpers.getGuiHelper(), unitsPerMilliBucket);
        registration.addRecipeCategories(category);
        RecipePage.LookupFilter filter = page.lookupFilter();
        if (filter != null) {
            lookupPlugins.put(
                    page.id(),
                    new JeiLookupPlugin<>(
                            page,
                            category,
                            filter,
                            helpers.getIngredientManager(),
                            helpers.getPlatformFluidHelper()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (RecipePage<?> page : pages()) addRows(registration, page);
        registration.addRecipes(RecipeTypes.CRAFTING, CraftingRows.rows());
    }

    private <T> void addRows(IRecipeRegistration registration, RecipePage<T> page) {
        JeiLookupPlugin<?> plugin = lookupPlugins.get(page.id());
        if (plugin == null) registration.addRecipes(typeOf(page), page.rows());
        else plugin.fill();
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        for (JeiLookupPlugin<?> plugin : lookupPlugins.values())
            addLookupPlugin(registration, plugin);
        // backport-fix: BF-063 — R on any multi fluid identifier shows its crafting recipe
        registration.addTypedRecipeManagerPlugin(RecipeTypes.CRAFTING, new FluidIdentifierRecipes());
    }

    private static <T> void addLookupPlugin(
            IAdvancedRegistration registration, JeiLookupPlugin<T> plugin) {
        // backport: JEI 19.x name of 26.x addSimpleRecipeManagerPlugin (since 19.16.0)
        registration.addTypedRecipeManagerPlugin(plugin.type(), plugin);
    }

    @Override
    public void registerItemSubtypes(@NonNull ISubtypeRegistration registration) {
        // backport: JEI 19.x ISubtypeInterpreter also has the (deprecated) legacy string form, so it is
        // no lambda; the legacy string is only read for old JEI config files (unverified: toString of
        // the subtype data is an adequate legacy key, 26.x JEI no longer has this method)
        Reg.visitItemSubtypes(
                (item, kind) ->
                        registration.registerSubtypeInterpreter(
                                item,
                                new ISubtypeInterpreter<ItemStack>() {
                                    @Override
                                    public @Nullable Object getSubtypeData(
                                            ItemStack stack, UidContext context) {
                                        return kind.getSubtypeData(stack);
                                    }

                                    @Override
                                    @SuppressWarnings("removal")
                                    public String getLegacyStringSubtypeInfo(
                                            ItemStack stack, UidContext context) {
                                        Object data = kind.getSubtypeData(stack);
                                        return data == null ? "" : data.toString();
                                    }
                                }));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        jeiRuntime
                .getIngredientManager()
                .removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, RecipePages.hiddenStacks());
        for (RecipePage<?> page : RecipePages.fixed()) hideRows(jeiRuntime, page);
    }

    private <T> void hideRows(IJeiRuntime runtime, RecipePage<T> page) {
        RecipeType<T> type = typeOf(page);
        List<T> hidden =
                runtime.getRecipeManager()
                        .createRecipeLookup(type)
                        .includeHidden()
                        .get()
                        .filter(page::hidden)
                        .toList();
        if (!hidden.isEmpty()) runtime.getRecipeManager().hideRecipes(type, hidden);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (RecipePage<?> page : pages()) {
            // backport: JEI 19.x addRecipeCatalysts (26.x JEI addCraftingStation)
            registration.addRecipeCatalysts(
                    typeOf(page), page.catalysts().toArray(ItemStack[]::new));
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(
                ScreenAnvil.class,
                new IGuiContainerHandler<ScreenAnvil>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(ScreenAnvil screen) {
                        return List.of(screen.recipePanelArea());
                    }
                });
        Map<ResourceLocation, RecipePage<?>> pages = new HashMap<>();
        for (RecipePage<?> page : RecipePages.fixed()) pages.put(page.id(), page);
        for (RecipePages.ClickArea area : RecipePages.clickAreas()) {
            RecipeType<?> type =
                    area.page().equals(RecipePages.VANILLA_SMELTING)
                            ? RecipeTypes.SMELTING
                            : typeOf(pages.get(area.page()));
            registration.addRecipeClickArea(
                    area.screen(), area.x(), area.y(), area.width(), area.height(), type);
        }
        registration.addGuiContainerHandler(
                ScreenMachineCustom.class,
                new IGuiContainerHandler<ScreenMachineCustom>() {
                    @Override
                    public Collection<IGuiClickableArea> getGuiClickableAreas(
                            ScreenMachineCustom screen, double mouseX, double mouseY) {
                        CustomMachinePage page =
                                customMachines.get(screen.getMenu().blockEntity().machineType);
                        return page == null
                                ? List.of()
                                : List.of(
                                        IGuiClickableArea.createBasic(
                                                RecipePages.CUSTOM_MACHINE_X,
                                                RecipePages.CUSTOM_MACHINE_Y,
                                                RecipePages.CUSTOM_MACHINE_WIDTH,
                                                RecipePages.CUSTOM_MACHINE_HEIGHT,
                                                typeOf(page)));
                    }
                });
        registration.addGhostIngredientHandler(
                ScreenMachineAutocrafter.class, new AutocrafterGhostHandler());
    }
}
