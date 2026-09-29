// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.hazard;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.transformer.IHazardTransformer;
import com.tiviacz.travelersbackpack.components.BackpackContainerContents;
import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.items.TravelersBackpackItem;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * Radiation of the items stored in a Traveler's Backpack (storage, tool slots, upgrades).
 *
 * <p>backport: ntm-next's com.hbm.hazard.transformer.HazardTransformerTravelersBackpack for
 * Traveler's Backpack 10.x (1.21.1). There the three containers are BackpackContainerContents
 * components, not vanilla ItemContainerContents as in the 26.x build, so the stacks are read
 * through getItems().
 */
public final class HazardTransformerTravelersBackpack implements IHazardTransformer {
    @Override
    public boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof TravelersBackpackItem
                && (stack.has(ModDataComponents.BACKPACK_CONTAINER.get())
                        || stack.has(ModDataComponents.TOOLS_CONTAINER.get())
                        || stack.has(ModDataComponents.UPGRADES.get()));
    }

    @Override
    public boolean readsLiveStorage() {
        return true;
    }

    @Override
    public void transform(ItemStack stack, List<HazardEntry> entries) {
        float radiation = sum(stack, ModDataComponents.BACKPACK_CONTAINER.get())
                + sum(stack, ModDataComponents.TOOLS_CONTAINER.get())
                + sum(stack, ModDataComponents.UPGRADES.get());
        if (radiation > 0F) entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
    }

    private static float sum(ItemStack stack, DataComponentType<BackpackContainerContents> type) {
        BackpackContainerContents contents = stack.get(type);
        return contents == null ? 0F : StorageHazardSum.sum(contents.getItems());
    }
}
