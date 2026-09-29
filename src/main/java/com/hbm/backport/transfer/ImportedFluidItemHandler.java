// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.TransferPreconditions;
import com.hbm.lib.neotransfer.access.ItemAccess;
import com.hbm.lib.neotransfer.fluid.FluidResource;
import com.hbm.lib.neotransfer.item.ItemResource;
import com.hbm.lib.neotransfer.transaction.TransactionContext;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a foreign item's 1.21.1 {@link IFluidHandlerItem} as the mod's {@code ItemAccess}-bound fluid handler.
 *
 * <p>Transactional without any journal of its own: each operation runs the foreign handler in EXECUTE mode on a
 * throw-away single-item COPY of the accessed stack, reads the resulting {@code getContainer()}, and writes the
 * change back through the {@link ItemAccess} by exchanging the items - which the access journals, so an abort
 * restores the original item. For stacks of N items the amount is split per item, as the 26.x
 * {@code ItemAccessResourceHandler} does.
 *
 * <p>If the copy reports a transfer but its container did not change, the handler keeps its state outside the
 * stack (creative/infinite or network-linked items). Such a transfer cannot be made transactional; it is undone
 * on the copy and refused (returns 0).
 *
 * <p>Stateless over the {@link ItemAccess}: several wrappers over the same access/slot in one transaction stay
 * consistent, so no per-slot wrapper cache is needed. Reported amounts are capped: a foreign handler that reports
 * more than was offered/requested per item is refused (0) and logged.
 */
final class ImportedFluidItemHandler implements ResourceHandler<FluidResource> {

    private final ItemAccess access;
    private final int size;

    ImportedFluidItemHandler(ItemAccess access, int size) {
        this.access = access;
        this.size = size;
    }

    static @Nullable IFluidHandlerItem handlerOf(ItemStack single) {
        return single.isEmpty() ? null : single.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
    }

    private @Nullable IFluidHandlerItem current() {
        ItemResource resource = access.getResource();
        return resource.isEmpty() ? null : handlerOf(resource.toStack(1));
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, size);
        IFluidHandlerItem h = current();
        return h == null || index >= h.getTanks() ? FluidResource.EMPTY : FluidResource.of(h.getFluidInTank(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size);
        IFluidHandlerItem h = current();
        if (h == null || index >= h.getTanks()) return 0;
        return (long) h.getFluidInTank(index).getAmount() * access.getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, size);
        IFluidHandlerItem h = current();
        if (h == null || index >= h.getTanks()) return 0;
        if (!resource.isEmpty() && !h.isFluidValid(index, resource.toStack(1000))) return 0;
        return (long) h.getTankCapacity(index) * access.getAmount();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, size);
        IFluidHandlerItem h = current();
        return h != null && index < h.getTanks() && !resource.isEmpty() && h.isFluidValid(index, resource.toStack(1000));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size);
        return insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size);
        return extract(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int count = access.getAmount();
        if (count <= 0 || amount < count) return 0;
        int perItem = amount / count;
        ItemResource before = access.getResource();
        if (before.isEmpty()) return 0;
        IFluidHandlerItem h = handlerOf(before.toStack(1));
        if (h == null) return 0;
        // Simulate first: for items whose fluid lives outside the stack every EXECUTE below really moves fluid in
        // that external storage and must be undone; skipping hopeless attempts keeps the mod's frequent probes
        // (aborted transactions) from churning it.
        if (h.fill(resource.toStack(perItem), FluidAction.SIMULATE) <= 0) return 0;
        int filled = h.fill(resource.toStack(perItem), FluidAction.EXECUTE);
        if (filled <= 0) return 0;
        ItemStack after = h.getContainer();
        if (filled > perItem) {
            // Over-report (contract violation): the copy's new state does not match any amount we could report.
            // Refuse; only the throw-away copy changed (unless the storage is external, see undo()).
            BridgeSupport.LOGGER.error(
                    "Fluid item bridge: {} reported filling {} mB of {} when {} mB were offered; refused",
                    before, filled, resource, perItem);
            undo(h, before, after, resource, filled, false);
            return 0;
        }
        if (after.getCount() == 1 && before.matches(after)) {
            undo(h, before, after, resource, filled, false);
            return 0;
        }
        return filled * BridgeSupport.exchange(access, before, count, after, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int count = access.getAmount();
        if (count <= 0 || amount < count) return 0;
        int perItem = amount / count;
        ItemResource before = access.getResource();
        if (before.isEmpty()) return 0;
        IFluidHandlerItem h = handlerOf(before.toStack(1));
        if (h == null) return 0;
        if (h.drain(resource.toStack(perItem), FluidAction.SIMULATE).isEmpty()) return 0; // see insert
        FluidStack drained = h.drain(resource.toStack(perItem), FluidAction.EXECUTE);
        if (drained.isEmpty()) return 0;
        ItemStack after = h.getContainer();
        if (!resource.matches(drained) || drained.getAmount() > perItem) {
            // Wrong fluid or over-report (contract violation): refuse; only the copy changed (see undo()).
            BridgeSupport.LOGGER.error(
                    "Fluid item bridge: {} drained {} when {} mB of {} were requested; refused",
                    before, drained, perItem, resource);
            undo(h, before, after, FluidResource.of(drained), drained.getAmount(), true);
            return 0;
        }
        if (after.getCount() == 1 && before.matches(after)) {
            undo(h, before, after, resource, drained.getAmount(), true);
            return 0;
        }
        return drained.getAmount() * BridgeSupport.exchange(access, before, count, after, transaction);
    }

    /**
     * Reverts an EXECUTE made on the throw-away copy. This only matters when the item's fluid is NOT stored in the
     * stack (the copy's container is unchanged): then the execute hit shared/external storage and must be undone
     * there. The undo can fall short (e.g. an insert-only network); that is logged as an error - unverified
     * whether any item in the modpack behaves like that.
     */
    private static void undo(
            IFluidHandlerItem h, ItemResource before, ItemStack after, FluidResource fluid, int amount, boolean refill) {
        if (after.getCount() != 1 || !before.matches(after)) return; // state is in the copy: discarding it is the undo
        int undone = refill
                ? h.fill(fluid.toStack(amount), FluidAction.EXECUTE)
                : h.drain(fluid.toStack(amount), FluidAction.EXECUTE).getAmount();
        if (undone != amount) {
            BridgeSupport.LOGGER.error(
                    "Fluid item bridge: could not undo a probe on {} (external storage): {} of {} mB {} reverted",
                    before, undone, amount, fluid);
        }
    }
}
