// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.recipeviewer;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.capability.FluidContainerRows;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemFluidBucket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Backport (GitHub #12; not in ntm-next): the "Fluid Containers" page 1.7.10 had in NEI
 * (FluidRecipeHandler, id {@code fluidcons}): which fluid and which empty container give which full
 * one. 1.7.10 read it off FluidContainerRegistry; the port has no such registry, so the rows are
 * gathered from where the containers live now:
 *
 * <ul>
 *   <li>{@link FluidContainerRows}: bottles, barrels, ores, mercury, ZIRNOX rods, cans, and the
 *       disperser canister / glyphid gland that fill from their own empty item;
 *   <li>buckets: water and lava (as 1.7.10 listed them) and the NTM fluid bucket;
 *   <li>every machine-convertible {@link IFluidContainerItem} that starts empty (tanks, barrels,
 *       lead tanks, canisters, gas tanks, cells, particle capsules, IV bags), once for each fluid it
 *       can store, at its full capacity.
 * </ul>
 *
 * <p>Slots as on the NEI page: fluid, empty container, arrow, full container. Containers that only
 * give a fluid (oil ore) have no empty slot.
 */
public final class FluidContainerPage extends RecipePage<FluidContainerPage.Fill> {

    FluidContainerPage() {
        super(PageIds.page("fluid_containers"), Fill.class);
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.fluid_containers");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModItems.FLUID_TANK);
    }

    /** The empty containers the NEI page named as its machines. */
    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModItems.FLUID_BARREL),
                new ItemStack(ModItems.FLUID_TANK),
                new ItemStack(ModItems.FLUID_TANK_LEAD),
                new ItemStack(ModItems.CANISTER),
                new ItemStack(ModItems.GAS_TANK),
                new ItemStack(ModItems.CELL_EMPTY),
                new ItemStack(ModItems.DISPERSER_CANISTER_EMPTY),
                new ItemStack(ModItems.GLYPHID_GLAND_EMPTY));
    }

    @Override
    public List<Fill> rows() {
        Map<ResourceLocation, Fill> rows = new LinkedHashMap<>();
        Set<Item> handled = new HashSet<>();

        for (FluidContainerRows.Row row : FluidContainerRows.rows())
            add(rows, row.fluid().get(), row.amount().getAsInt(), row.emptyStack(), row.fullStack());

        FluidContainerRows.forEachFamilyFill(
                (empty, family) -> {
                    handled.add(family);
                    for (Fluid fluid : NTMFluids.displayOrder()) {
                        if (!family.canStore(fluid)) continue;
                        add(rows, fluid, family.capacity, new ItemStack(empty), family.make(fluid, family.capacity));
                    }
                });

        ItemStack bucket = new ItemStack(Items.BUCKET);
        add(rows, NTMFluids.WATER, ItemFluidBucket.VOLUME, bucket, new ItemStack(Items.WATER_BUCKET));
        add(rows, NTMFluids.LAVA, ItemFluidBucket.VOLUME, bucket, new ItemStack(Items.LAVA_BUCKET));
        ItemFluidBucket ntmBucket = ModItems.FLUID_BUCKET.get();
        handled.add(ntmBucket);
        for (Fluid fluid : NTMFluids.displayOrder()) {
            if (ntmBucket.canStore(fluid))
                add(rows, fluid, ItemFluidBucket.VOLUME, bucket, ItemFluidBucket.make(fluid));
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof IFluidContainerItem container)
                    || handled.contains(item)
                    || !container.machineConvertible()) continue;
            ItemStack empty = new ItemStack(item);
            // a full fixed item (a filled cell, an IV bag of blood) is reached from its empty one
            if (container.getContent(empty).type() != Fluids.EMPTY) continue;
            int capacity = container.capacity(empty);
            if (capacity <= 0) continue;
            for (Fluid fluid : NTMFluids.displayOrder()) {
                if (!container.canStore(fluid)) continue;
                ItemStack full;
                try {
                    full = container.filledContainer(empty, new FluidStackNTM(fluid, capacity));
                } catch (IllegalArgumentException | UnsupportedOperationException refused) {
                    continue;
                }
                add(rows, fluid, capacity, empty, full);
            }
        }
        return new ArrayList<>(rows.values());
    }

    private void add(Map<ResourceLocation, Fill> rows, Fluid fluid, int amount, ItemStack empty, ItemStack full) {
        if (fluid == null || fluid == Fluids.EMPTY || full.isEmpty()) return;
        ResourceLocation id =
                PageIds.derived(
                        id(),
                        PageIds.segment(BuiltInRegistries.ITEM.getKey(full.getItem())),
                        PageIds.segment(BuiltInRegistries.FLUID.getKey(fluid)));
        rows.putIfAbsent(id, new Fill(id, fluid, amount, empty.copyWithCount(1), full.copyWithCount(1)));
    }

    @Override
    public ResourceLocation rowId(Fill row) {
        return row.id();
    }

    @Override
    public void layout(Fill row, PageLayout page) {
        page.input(30, 24).background().fluid(row.fluid(), row.amount());
        if (!row.empty().isEmpty()) page.input(48, 24).background().item(row.empty());
        page.output(102, 24).background().item(row.full());
    }

    @Override
    public void draw(Fill row, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    /** {@code amount} mB of {@code fluid} into {@code empty} (none: the full item only gives it) makes {@code full}. */
    public record Fill(ResourceLocation id, Fluid fluid, int amount, ItemStack empty, ItemStack full) {}
}
