// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.misc;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges 26.x block tint source lists (NeoForge 26 {@code RegisterColorHandlersEvent
 * .BlockTintSources#register(List, Block...)}) to 1.21.1 {@link BlockColor}s registered through
 * {@link RegisterColorHandlersEvent.Block}.
 */
public final class BlockTints {

    private static final Logger LOGGER = LoggerFactory.getLogger("hbm-backport");
    private static Field coloringStates;
    private static boolean coloringStatesFailed;

    private BlockTints() {}

    /** tint index i -> sources.get(i); an index outside the list is untinted (-1 = white). */
    public static BlockColor color(List<BlockTintSource> sources) {
        BlockTintSource[] layers = sources.toArray(new BlockTintSource[0]);
        return (state, level, pos, tintIndex) -> {
            if (tintIndex < 0 || tintIndex >= layers.length) return -1;
            BlockTintSource source = layers[tintIndex];
            // backport: 1.21.1 passes a null level/pos when there is no world context; that is
            // the 26.x color(state) case. Alpha is ignored by 1.21.1 tinting (26.x ARGB values
            // keep their RGB).
            return level != null && pos != null
                    ? source.colorInWorld(state, level, pos)
                    : source.color(state);
        };
    }

    /** 26.x {@code event.register(sources, blocks)} on the 1.21.1 event. */
    public static void register(
            RegisterColorHandlersEvent.Block event, List<BlockTintSource> sources, Block... blocks) {
        event.register(color(sources), blocks);
        Set<Property<?>> props = new HashSet<>();
        for (BlockTintSource source : sources) props.addAll(source.relevantProperties());
        if (!props.isEmpty()) addColoringStates(event.getBlockColors(), props, blocks);
    }

    /**
     * 1.21.1 keeps the tint-relevant properties in BlockColors' private {@code coloringStates}
     * (read by BlockStateModelLoader to decide whether a state change needs a re-mesh); its
     * adder is private. backport: reflection instead of an accessor mixin (runtime uses
     * Mojang names on NeoForge 1.21.1).
     */
    @SuppressWarnings("unchecked")
    private static void addColoringStates(BlockColors colors, Set<Property<?>> props, Block... blocks) {
        if (coloringStatesFailed) return;
        try {
            if (coloringStates == null) {
                coloringStates = BlockColors.class.getDeclaredField("coloringStates");
                coloringStates.setAccessible(true);
            }
            Map<Block, Set<Property<?>>> map = (Map<Block, Set<Property<?>>>) coloringStates.get(colors);
            Set<Property<?>> copy = Set.copyOf(props);
            for (Block block : blocks) map.put(block, copy);
        } catch (ReflectiveOperationException | RuntimeException e) {
            coloringStatesFailed = true;
            LOGGER.warn("Could not register tint-relevant block properties", e);
        }
    }
}
