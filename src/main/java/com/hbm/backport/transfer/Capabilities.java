// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.transfer;

import com.hbm.lib.neotransfer.ResourceHandler;
import com.hbm.lib.neotransfer.access.ItemAccess;
import com.hbm.lib.neotransfer.energy.EnergyHandler;
import com.hbm.lib.neotransfer.fluid.FluidResource;
import com.hbm.lib.neotransfer.item.ItemResource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jspecify.annotations.Nullable;

/**
 * Backport stand-in for NeoForge 26.x {@code net.neoforged.neoforge.capabilities.Capabilities}, limited to
 * the members the mod uses. These are the mod's OWN capabilities (namespace {@code hbm}) typed with the
 * backported transfer API; {@link TransferBridge#register} connects them to 1.21.1's
 * {@code IItemHandler}/{@code IFluidHandler}/{@code IEnergyStorage} capabilities in both directions.
 */
public final class Capabilities {

    private Capabilities() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("hbm", path);
    }

    public static final class Item {
        private Item() {}

        public static final BlockCapability<ResourceHandler<ItemResource>, @Nullable Direction> BLOCK =
                BlockCapability.createSided(id("transfer_item"), ResourceHandler.<ItemResource>asClass());
    }

    public static final class Fluid {
        private Fluid() {}

        public static final BlockCapability<ResourceHandler<FluidResource>, @Nullable Direction> BLOCK =
                BlockCapability.createSided(id("transfer_fluid"), ResourceHandler.<FluidResource>asClass());

        public static final ItemCapability<ResourceHandler<FluidResource>, ItemAccess> ITEM =
                ItemCapability.create(
                        id("transfer_fluid_item"), ResourceHandler.<FluidResource>asClass(), ItemAccess.class);
    }

    public static final class Energy {
        private Energy() {}

        public static final BlockCapability<EnergyHandler, @Nullable Direction> BLOCK =
                BlockCapability.createSided(id("transfer_energy"), EnergyHandler.class);

        public static final ItemCapability<EnergyHandler, ItemAccess> ITEM =
                ItemCapability.create(id("transfer_energy_item"), EnergyHandler.class, ItemAccess.class);
    }
}
