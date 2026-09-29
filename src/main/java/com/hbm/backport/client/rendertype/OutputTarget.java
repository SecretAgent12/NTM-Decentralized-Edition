// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import net.minecraft.client.renderer.RenderStateShard;

/**
 * 26.x {@code net.minecraft.client.renderer.rendertype.OutputTarget}: which frame target a type draws
 * into. backport: wraps the 1.21.1 output shard.
 */
public final class OutputTarget {
    public static final OutputTarget MAIN_TARGET = new OutputTarget(RenderStateShard.MAIN_TARGET);
    public static final OutputTarget OUTLINE_TARGET = new OutputTarget(RenderStateShard.OUTLINE_TARGET);
    public static final OutputTarget TRANSLUCENT_TARGET = new OutputTarget(RenderStateShard.TRANSLUCENT_TARGET);
    public static final OutputTarget PARTICLES_TARGET = new OutputTarget(RenderStateShard.PARTICLES_TARGET);
    public static final OutputTarget WEATHER_TARGET = new OutputTarget(RenderStateShard.WEATHER_TARGET);
    public static final OutputTarget CLOUDS_TARGET = new OutputTarget(RenderStateShard.CLOUDS_TARGET);
    public static final OutputTarget ITEM_ENTITY_TARGET = new OutputTarget(RenderStateShard.ITEM_ENTITY_TARGET);

    private final RenderStateShard.OutputStateShard shard;

    public OutputTarget(RenderStateShard.OutputStateShard shard) {
        this.shard = shard;
    }

    public RenderStateShard.OutputStateShard shard() {
        return shard;
    }
}
