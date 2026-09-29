// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import com.hbm.backport.client.itemmodel.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A visualizer keyed to an item: hosts draw its stacks through it instead of the item's renderer.
 * Registered with VisualizerRegistry#setVisualizer(Item, ItemStackVisualizer) (backport: the
 * com.hbm.backport.client.flywheel.VisualizerRegistry facade). First-person and GUI rendering stay with the item's
 * renderer.
 */
public interface ItemStackVisualizer {
    /**
     * Called by the host, on the host's threads.
     *
     * @param ctx   the host's context; instances belong to the host.
     * @param owner the holder, item entity, frame or display, when there is one.
     */
    ItemStackVisual createVisual(VisualizationContext ctx, ItemStack stack, ItemDisplayContext displayContext,
                                 @Nullable ItemOwner owner);
}
