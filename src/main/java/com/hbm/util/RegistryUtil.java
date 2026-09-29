// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.lib.Library;
import java.util.NoSuchElementException;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class RegistryUtil {

    private RegistryUtil() {}

    public static <T> T valueOrThrow(Registry<T> registry, ResourceLocation id) {
        return registry.getOrThrow(ResourceKey.create(registry.key(), id));
    }

    public static <T> T valueOrThrow(Registry<T> registry, String reference) {
        return valueOrThrow(registry, Library.resolve(reference));
    }

    public static <T> ResourceLocation keyOrThrow(Registry<T> registry, T value) {

        ResourceLocation id = registry.getKeyOrNull(value);
        if (id == null) throw new NoSuchElementException();
        return id;
    }

    public static ResourceLocation keyOf(Block block) {
        return keyOrThrow(BuiltInRegistries.BLOCK, block);
    }

    public static ResourceLocation keyOf(Fluid fluid) {
        return keyOrThrow(BuiltInRegistries.FLUID, fluid);
    }

    public static Block block(String reference) {
        return valueOrThrow(BuiltInRegistries.BLOCK, reference);
    }

    public static Item item(String reference) {
        return valueOrThrow(BuiltInRegistries.ITEM, reference);
    }

    public static Fluid fluid(ResourceLocation id) {
        return valueOrThrow(BuiltInRegistries.FLUID, id);
    }
}
