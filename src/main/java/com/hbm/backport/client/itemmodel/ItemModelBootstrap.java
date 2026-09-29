// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.client.ClientRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * backport: client registration of the 26.x item model system on 1.21.1. Replaces the 26.x
 * RegisterItemModelsEvent / RegisterSpecialModelRendererEvent / ItemTintSources /
 * SelectItemModelProperties registrations of the client entry point.
 */
public final class ItemModelBootstrap {
    private ItemModelBootstrap() {}

    private static boolean hbm(Item item) {
        return "hbm".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    public static void register(IEventBus modBus) {
        ClientRegistry.registerItemModels(ItemModels.ID_MAPPER::put);
        ClientRegistry.registerSpecialModelRenderers(SpecialModelRenderers.ID_MAPPER::put);
        ClientRegistry.registerItemTintSources(ItemTintSources.ID_MAPPER::put);
        ClientRegistry.registerSelectItemModelProperties(SelectItemModelProperties.ID_MAPPER::put);

        modBus.addListener(ModelEvent.RegisterAdditional.class, ClientItems::onRegisterAdditional);
        modBus.addListener(ModelEvent.ModifyBakingResult.class, ClientItems::onModifyBakingResult);
        modBus.addListener(ModelEvent.BakingCompleted.class, ClientItems::onBakingCompleted);
        modBus.addListener(
                (RegisterColorHandlersEvent.Item event) ->
                        event.register(
                                ClientItems::nativeTint,
                                BuiltInRegistries.ITEM.stream().filter(ItemModelBootstrap::hbm).toArray(Item[]::new)));
        // LOWEST: items that already got extensions elsewhere (guns: GunClientExtensions) must return
        // ItemDefinitionRenderer.get() from their own getCustomRenderer()
        modBus.addListener(
                EventPriority.LOWEST,
                (RegisterClientExtensionsEvent event) ->
                        event.registerItem(
                                ItemDefinitionRenderer.EXTENSIONS,
                                BuiltInRegistries.ITEM.stream()
                                        .filter(ItemModelBootstrap::hbm)
                                        .filter(item -> !event.isItemRegistered(item))
                                        .toArray(Item[]::new)));
    }
}
