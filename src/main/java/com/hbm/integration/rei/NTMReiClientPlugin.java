// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.rei;

import com.hbm.client.gui.ScreenAnvil;
import com.hbm.client.gui.ScreenMachineCustom;
import com.hbm.integration.recipeviewer.CustomMachinePage;
import com.hbm.integration.recipeviewer.RecipePage;
import com.hbm.integration.recipeviewer.RecipePages;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.items.ModItems;
import com.hbm.registration.ItemSubtype;
import com.hbm.registration.Reg;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ClickArea;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/**
 * REI integration (our own, not in ntm-next or 1.7.10): the recipe pages of {@code
 * integration/recipeviewer} in Roughly Enough Items, alongside the JEI plugin that shows the same
 * pages in JEI. Only one of the two is ever loaded: each only by its own viewer.
 *
 * <ul>
 *   <li>one category per page (fixed pages and one per custom machine), the page's catalysts as
 *       workstations, one display per row; secret rows are left out while the config hides them;
 *   <li>click areas on machine screens, the custom machine's by the machine it is;
 *   <li>the anvil's recipe panel kept clear of REI's overlay;
 *   <li>item subtypes (fluid identifiers, batteries, bedrock ores...) told apart as in JEI;
 *   <li>hidden items left out of the list, the blank fluid identifier put in;
 *   <li>crafting lookups REI can't make itself ({@link ReiCraftingLookups}).
 * </ul>
 *
 * <p>Not carried over from JEI: the fuel pool page's hot/cold rod lookup filter (REI shows the row
 * either way), and dragging ghost stacks into the autocrafter.
 *
 * <p>With REI Plugin Compatibilities installed, REI also runs JEI plugins, and the pages show up
 * twice; that is a choice of the player's mod list, not something this plugin can tell.
 */
@REIPluginClient
public final class NTMReiClientPlugin implements REIClientPlugin {

    private final Map<ResourceKey<CustomMachineDefinition>, CustomMachinePage> customMachines =
            new LinkedHashMap<>();

    private List<RecipePage<?>> pages() {
        List<RecipePage<?>> pages = new ArrayList<>(RecipePages.fixed());
        pages.addAll(customMachines.values());
        return pages;
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        customMachines.clear();
        for (CustomMachinePage page : RecipePages.customMachines()) customMachines.put(page.key(), page);
        for (RecipePage<?> page : pages()) addCategory(registry, page);
    }

    private static <T> void addCategory(CategoryRegistry registry, RecipePage<T> page) {
        ReiPageCategory<T> category = new ReiPageCategory<>(page);
        registry.add(category);
        List<ItemStack> catalysts = page.catalysts();
        if (catalysts.isEmpty()) return;
        EntryStack<?>[] stations = new EntryStack<?>[catalysts.size()];
        for (int i = 0; i < stations.length; i++) stations[i] = EntryStacks.of(catalysts.get(i));
        registry.addWorkstations(category.id(), stations);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (RecipePage<?> page : pages()) addDisplays(registry, page);
        registry.registerDisplayGenerator(BuiltinPlugin.CRAFTING, new ReiCraftingLookups());
    }

    private static <T> void addDisplays(DisplayRegistry registry, RecipePage<T> page) {
        CategoryIdentifier<ReiPageDisplay<T>> id = ReiPageCategory.idOf(page);
        for (T row : page.rows()) {
            if (page.hidden(row)) continue;
            registry.add(new ReiPageDisplay<>(page, id, row));
        }
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        for (RecipePages.ClickArea area : RecipePages.clickAreas()) {
            CategoryIdentifier<?> category =
                    area.page().equals(RecipePages.VANILLA_SMELTING)
                            ? BuiltinPlugin.SMELTING
                            : CategoryIdentifier.of(area.page());
            containerClickArea(
                    registry,
                    new Rectangle(area.x(), area.y(), area.width(), area.height()),
                    area.screen(),
                    category);
        }
        registry.registerClickArea(ScreenMachineCustom.class, this::customMachineArea);
    }

    /** REI's container click areas are typed by menu; the page table only knows the screen class. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void containerClickArea(
            ScreenRegistry registry, Rectangle area, Class screen, CategoryIdentifier<?> category) {
        registry.registerContainerClickArea(area, screen, category);
    }

    private ClickArea.Result customMachineArea(
            ClickArea.ClickAreaContext<ScreenMachineCustom> context) {
        ScreenMachineCustom screen = context.getScreen();
        CustomMachinePage page = customMachines.get(screen.getMenu().blockEntity().machineType);
        if (page == null) return ClickArea.Result.fail();
        {
            Point mouse = context.getMousePosition();
            int x = mouse.x - screen.getGuiLeft();
            int y = mouse.y - screen.getGuiTop();
            if (x < RecipePages.CUSTOM_MACHINE_X
                    || y < RecipePages.CUSTOM_MACHINE_Y
                    || x >= RecipePages.CUSTOM_MACHINE_X + RecipePages.CUSTOM_MACHINE_WIDTH
                    || y >= RecipePages.CUSTOM_MACHINE_Y + RecipePages.CUSTOM_MACHINE_HEIGHT)
                return ClickArea.Result.fail();
        }
        return ClickArea.Result.success().category(ReiPageCategory.idOf(page));
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        ExclusionZonesProvider<ScreenAnvil> anvil =
                screen -> {
                    Rect2i panel = screen.recipePanelArea();
                    return List.of(
                            new Rectangle(
                                    panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight()));
                };
        zones.register(ScreenAnvil.class, anvil);
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        Reg.visitItemSubtypes(
                (item, kind) -> registry.register((context, stack) -> subtypeHash(kind, stack), item));
    }

    private static long subtypeHash(ItemSubtype kind, ItemStack stack) {
        return Objects.hashCode(kind.getSubtypeData(stack)) ^ 0x2610_3051L;
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        List<ItemStack> hidden = RecipePages.hiddenStacks();
        registry.removeEntryIf(
                (EntryStack<?> entry) ->
                        entry.getValue() instanceof ItemStack stack && isHidden(hidden, stack));
        EntryStack<ItemStack> blank = EntryStacks.of(new ItemStack(ModItems.FLUID_IDENTIFIER));
        if (!registry.alreadyContain(blank)) registry.addEntry(blank);
    }

    private static boolean isHidden(List<ItemStack> hidden, ItemStack stack) {
        for (ItemStack match : hidden) {
            if (!stack.is(match.getItem())) continue;
            // a plain hidden item hides every stack of it; one with data only that stack
            if (match.isComponentsPatchEmpty() || ItemStack.isSameItemSameComponents(match, stack))
                return true;
        }
        return false;
    }
}
