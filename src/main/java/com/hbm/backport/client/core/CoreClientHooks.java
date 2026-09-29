// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.hbm.client.render.ManlyPlayerLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client registration the core render bridge needs in 1.21.1 (mod event bus).
 *
 * <p>26.x adds {@link ManlyPlayerLayer} from a mixin into AvatarRenderer's constructor; 1.21.1 has
 * the NeoForge AddLayers event for the player renderers instead.
 */
public final class CoreClientHooks {
    private CoreClientHooks() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(CoreClientHooks::addLayers);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new ManlyPlayerLayer(renderer));
            }
        }
    }
}
