// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.misc;

import com.hbm.client.ClientRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client registration for the clientmisc topic; call from the client entry point (mod bus). */
public final class MiscClientHooks {

    private MiscClientHooks() {}

    public static void register(IEventBus modBus) {
        // replaces the NeoForge 26 RegisterColorHandlersEvent.BlockTintSources listener
        modBus.addListener(
                (RegisterColorHandlersEvent.Block event) ->
                        ClientRegistry.registerBlockTintSources(
                                (sources, blocks) -> BlockTints.register(event, sources, blocks)));
        // replaces MixinFogRenderer's SootFog hook
        SootFogEvents.register();
    }
}
