// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.backport.client.rendertype.ChunkSectionLayer;
import com.hbm.platform.Services;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

final class SectionGeometryShaders {
    private static final boolean IRIS = Services.PLATFORM.isModLoaded("iris");

    private SectionGeometryShaders() {}

    static @Nullable ChunkSectionLayer layer(BlockState state) {
        return IRIS ? IrisLayer.layer(state) : null;
    }

    // backport: Iris is not on the 1.21.1 compile classpath; the same Iris API
    // (WorldRenderingSettings.INSTANCE.getBlockTypeIds(): Map<Block, BlockRenderType>) is read
    // reflectively
    private static final class IrisLayer {
        private static final @Nullable Object SETTINGS;
        private static final @Nullable Method TYPE_IDS;

        static {
            Object settings = null;
            Method typeIds = null;
            try {
                Class<?> type =
                        Class.forName(
                                "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings");
                Field instance = type.getField("INSTANCE");
                settings = instance.get(null);
                typeIds = type.getMethod("getBlockTypeIds");
            } catch (ReflectiveOperationException | LinkageError e) {
                settings = null;
                typeIds = null;
            }
            SETTINGS = settings;
            TYPE_IDS = typeIds;
        }

        static @Nullable ChunkSectionLayer layer(BlockState state) {
            if (SETTINGS == null || TYPE_IDS == null) return null;
            Object types;
            try {
                types = TYPE_IDS.invoke(SETTINGS);
            } catch (ReflectiveOperationException e) {
                return null;
            }
            if (!(types instanceof Map<?, ?> map)) return null;
            Object type = map.get(state.getBlock());
            if (!(type instanceof Enum<?> value)) return null;
            return switch (value.name()) {
                case "SOLID" -> ChunkSectionLayer.SOLID;
                case "CUTOUT", "CUTOUT_MIPPED" -> ChunkSectionLayer.CUTOUT;
                case "TRANSLUCENT" -> ChunkSectionLayer.TRANSLUCENT;
                default -> null;
            };
        }
    }
}
