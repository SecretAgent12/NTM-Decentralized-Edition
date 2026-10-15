// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.NuclearTech;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.Nbt;

public final class MultiblockLayouts {

    private record Source(Class<?> anchor, String namespace) {}

    private static final List<Source> SOURCES =
            new ArrayList<>(List.of(new Source(NuclearTech.class, NuclearTech.MOD_ID)));
    private static @Nullable Map<ResourceLocation, MultiblockLayout> loaded;
    private static boolean released;

    private MultiblockLayouts() {}

    public static String resource(String namespace) {
        return "/" + namespace + "/multiblock_layouts.nbt";
    }

    public static synchronized void addSource(Class<?> anchor, String namespace) {
        if (loaded != null || released) {
            throw new IllegalStateException(
                    namespace + " registered its layouts after they loaded");
        }
        SOURCES.add(new Source(anchor, namespace));
    }

    public static synchronized MultiblockLayout of(BlockMultiblockCore block) {
        if (released)
            throw new IllegalStateException(
                    "multiblock layouts asked for " + block + " after their release");
        if (loaded == null) loaded = load();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        MultiblockLayout layout = loaded.get(id);
        if (layout == null) {
            throw new IllegalStateException(
                    "no baked multiblock layout for "
                            + id
                            + " - run :neoforge:runData / :fabric:runDatagen");
        }
        return layout;
    }

    public static synchronized void release() {
        loaded = null;
        released = true;
    }

    private static Map<ResourceLocation, MultiblockLayout> load() {
        Map<ResourceLocation, MultiblockLayout> all = new HashMap<>();
        Map<ResourceLocation, String> overriddenBy = new HashMap<>();
        for (Source source : SOURCES) {
            String resource = resource(source.namespace());
            try (InputStream in = source.anchor().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException(
                            "missing the baked multiblock layouts at "
                                    + resource
                                    + " - run :neoforge:runData / :fabric:runDatagen");
                }
                read(in).forEach(
                                (id, layout) -> {
                                    if (!id.getNamespace().equals(source.namespace())) {
                                        String prior = overriddenBy.put(id, source.namespace());
                                        if (prior != null) {
                                            throw new IllegalStateException(
                                                    prior
                                                            + " and "
                                                            + source.namespace()
                                                            + " both reshape "
                                                            + id);
                                        }
                                    }
                                    all.put(id, layout);
                                });
            } catch (IOException e) {
                throw new UncheckedIOException("reading " + resource, e);
            }
        }
        return all;
    }

    private static Map<ResourceLocation, MultiblockLayout> read(InputStream in) throws IOException {
        CompoundTag root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        Map<ResourceLocation, MultiblockLayout> layouts = new HashMap<>();
        for (String key : root.getAllKeys()) {
            layouts.put(ResourceLocation.parse(key), MultiblockLayout.read(Nbt.getCompoundOrEmpty(root, key)));
        }
        return layouts;
    }
}
