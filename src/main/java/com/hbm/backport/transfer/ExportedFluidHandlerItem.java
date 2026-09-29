// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.fluid.FluidResource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * 1.21.1 {@link IFluidHandlerItem} over one of the mod's {@code ItemAccess}-bound fluid item handlers. The
 * handler was obtained with a {@link ContainerItemAccess} over the queried stack, so {@link #getContainer()}
 * reflects filled/emptied/replaced containers (see {@link ContainerItemAccess} for in-place write-back).
 */
final class ExportedFluidHandlerItem extends ExportedFluidHandler implements IFluidHandlerItem {

    private final ContainerItemAccess access;

    ExportedFluidHandlerItem(ResourceHandler<FluidResource> handler, ContainerItemAccess access) {
        super(handler);
        this.access = access;
    }

    @Override
    public ItemStack getContainer() {
        return access.container();
    }
}
