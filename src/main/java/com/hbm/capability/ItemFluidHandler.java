// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.hbm.lib.neotransfer.ItemAccessResourceHandler;
import com.hbm.lib.neotransfer.access.ItemAccess;
import com.hbm.lib.neotransfer.fluid.FluidResource;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class ItemFluidHandler extends ItemAccessResourceHandler<FluidResource> {

    public ItemFluidHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        return FluidResource.of(ForeignFluidItemAccess.heldFluid(accessResource.toStack()));
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return (int) ForeignFluidItemAccess.heldMb(accessResource.toStack());
    }

    @Override
    protected @Nullable ItemResource update(
            ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        ItemStack updated =
                ForeignFluidItemAccess.withContent(
                        accessResource.toStack(),
                        newAmount <= 0 ? Fluids.EMPTY : newResource.getFluid(),
                        newAmount);
        return ItemResource.of(updated);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return getCapacity(index, resource) > 0;
    }

    @Override
    public int insert(
            int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty()) return 0;
        // backport-fix: BF-084 (DE 2026-10-10, commit 55668394) the item answers per item; the base class splits a stack-wide amount
        int count = itemAccess.getAmount();
        if (count <= 0) return 0;
        long accepted =
                ForeignFluidItemAccess.insertableMb(
                        itemAccess.getResource().toStack(), resource.getFluid(), amount / count);
        return accepted <= 0 ? 0 : super.insert(index, resource, stackWide(accepted, count), transaction);
    }

    @Override
    public int extract(
            int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty()) return 0;
        // backport-fix: BF-084 (DE 2026-10-10, commit 55668394) the item answers per item; the base class splits a stack-wide amount
        int count = itemAccess.getAmount();
        if (count <= 0) return 0;
        long given =
                ForeignFluidItemAccess.extractableMb(
                        itemAccess.getResource().toStack(), resource.getFluid(), amount / count);
        return given <= 0 ? 0 : super.extract(index, resource, stackWide(given, count), transaction);
    }

    /**
     * backport-fix: BF-084 — insert/extract amounts are for the whole stack (ItemAccess.getAmount
     * items), ItemAccessResourceHandler divides them by the count, but the item's own fill/drain
     * answer for one item. Passing that one-item answer on as the stack-wide amount drained a
     * stack of 16 deuterium cells by 1000/16 = 62 mB each, and a fixed 1000 mB cell has no item for
     * 938 mB: ItemFluidCell.filledContainer threw (EvilCraft's HUD drains the held stack every
     * frame and crashed the client). Same in ntm-next 1.0.0. NEXT 1.0.3 (2026-10-12) multiplies by the
     * count too, but as {@code (int) accepted * count} (int overflow); DE keeps its own version.
     */
    private static int stackWide(long perItem, int count) {
        return (int) Math.min(perItem * count, Integer.MAX_VALUE);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        ItemStack stack = itemAccess.getResource().toStack();

        Fluid fluid =
                resource.isEmpty() ? ForeignFluidItemAccess.heldFluid(stack) : resource.getFluid();
        return (int) ForeignFluidItemAccess.capacityMb(stack, fluid);
    }
}
