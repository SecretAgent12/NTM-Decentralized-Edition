// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import java.lang.reflect.Field;
import java.util.SortedSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.level.BlockDestructionProgress;

/** 1.21.1 homes of level render data 26.x exposes elsewhere. */
public final class LevelRenderAccess {
    private LevelRenderAccess() {}

    private static Field destructionProgress;

    /**
     * 26.x {@code ClientLevel.destructionProgress()}: 1.21.1 keeps the map on the LevelRenderer
     * (private; NeoForge runs with Mojang names, so it is read by name).
     */
    @SuppressWarnings("unchecked")
    public static Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress(ClientLevel level) {
        try {
            if (destructionProgress == null) {
                Field f = LevelRenderer.class.getDeclaredField("destructionProgress");
                f.setAccessible(true);
                destructionProgress = f;
            }
            return (Long2ObjectMap<SortedSet<BlockDestructionProgress>>)
                    destructionProgress.get(Minecraft.getInstance().levelRenderer);
        } catch (ReflectiveOperationException e) {
            return Long2ObjectMaps.emptyMap();
        }
    }
}
