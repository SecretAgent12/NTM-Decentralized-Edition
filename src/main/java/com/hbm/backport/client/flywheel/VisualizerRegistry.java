// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.hbm.lib.crankshaft.ItemStackVisualizer;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

/**
 * backport: Flywheel 1.0's VisualizerRegistry plus CrankShaft's item visualizers. Flywheel 1.0 has no item hosts of its
 * own, so item visualizers are only consulted by NTM visuals (ItemStackSlot); vanilla item rendering is unaffected.
 */
public final class VisualizerRegistry {
    private static volatile Map<Item, ItemStackVisualizer> items = new IdentityHashMap<>();

    private VisualizerRegistry() {}

    public static <T extends BlockEntity> @Nullable BlockEntityVisualizer<? super T> getVisualizer(BlockEntityType<T> type) {
        return dev.engine_room.flywheel.api.visualization.VisualizerRegistry.getVisualizer(type);
    }

    public static <T extends Entity> @Nullable EntityVisualizer<? super T> getVisualizer(EntityType<T> type) {
        return dev.engine_room.flywheel.api.visualization.VisualizerRegistry.getVisualizer(type);
    }

    public static <T extends BlockEntity> void setVisualizer(BlockEntityType<T> type, @Nullable BlockEntityVisualizer<? super T> visualizer) {
        dev.engine_room.flywheel.api.visualization.VisualizerRegistry.setVisualizer(type, visualizer);
    }

    public static <T extends Entity> void setVisualizer(EntityType<T> type, @Nullable EntityVisualizer<? super T> visualizer) {
        dev.engine_room.flywheel.api.visualization.VisualizerRegistry.setVisualizer(type, visualizer);
    }

    public static @Nullable ItemStackVisualizer getVisualizer(Item item) {
        return items.get(item);
    }

    /** Copy-on-write: set on the render thread during resource reload, read from visual worker threads. */
    public static synchronized void setVisualizer(Item item, @Nullable ItemStackVisualizer visualizer) {
        Map<Item, ItemStackVisualizer> next = new IdentityHashMap<>(items);
        if (visualizer == null) next.remove(item);
        else next.put(item, visualizer);
        items = next;
    }
}
