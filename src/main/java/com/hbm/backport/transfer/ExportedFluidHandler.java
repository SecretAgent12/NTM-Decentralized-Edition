// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.fluid.FluidResource;
import com.hbm.lib.neotransfer.transaction.Transaction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Exposes a transfer-API fluid handler as a 1.21.1 {@link IFluidHandler}. Each call is one transaction:
 * {@code SIMULATE} aborts, {@code EXECUTE} commits (see {@link BridgeSupport#open(boolean)}). Fluid stacks returned by
 * {@link #getFluidInTank} are fresh copies.
 */
class ExportedFluidHandler implements IFluidHandler {

    final ResourceHandler<FluidResource> handler;

    ExportedFluidHandler(ResourceHandler<FluidResource> handler) {
        this.handler = handler;
    }

    @Override
    public int getTanks() {
        return handler.size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        FluidResource resource = handler.getResource(tank);
        int amount = handler.getAmountAsInt(tank);
        return resource.isEmpty() || amount <= 0 ? FluidStack.EMPTY : resource.toStack(amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return handler.getCapacityAsInt(tank, handler.getResource(tank));
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return !stack.isEmpty() && handler.isValid(tank, FluidResource.of(stack));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        try (Transaction tx = BridgeSupport.open(action.execute())) {
            if (tx == null) return 0;
            int filled = handler.insert(FluidResource.of(resource), resource.getAmount(), tx);
            if (action.execute()) tx.commit();
            return filled;
        }
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        FluidResource fluid = FluidResource.of(resource);
        return drain(fluid, resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        int size = handler.size();
        for (int tank = 0; tank < size; tank++) {
            FluidResource fluid = handler.getResource(tank);
            if (fluid.isEmpty() || handler.getAmountAsLong(tank) <= 0) continue;
            FluidStack drained = drain(fluid, maxDrain, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }

    private FluidStack drain(FluidResource fluid, int amount, FluidAction action) {
        try (Transaction tx = BridgeSupport.open(action.execute())) {
            if (tx == null) return FluidStack.EMPTY;
            int drained = handler.extract(fluid, amount, tx);
            if (action.execute()) tx.commit();
            return drained > 0 ? fluid.toStack(drained) : FluidStack.EMPTY;
        }
    }
}
