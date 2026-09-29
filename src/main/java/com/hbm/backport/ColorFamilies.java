// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 26.x groups the sixteen dyed variants of a block or item (Blocks.WOOL.green(),
 * Items.DYE.white(), ...); 1.21.1 has one constant per colour (GREEN_WOOL). The
 * families the tree uses, resolved by the vanilla ids "<colour>_<suffix>".
 */
public final class ColorFamilies {

    private ColorFamilies() {}

    public static final Family<Block> WOOL = new Family<>(BuiltInRegistries.BLOCK, "wool");
    public static final Family<Block> BED = new Family<>(BuiltInRegistries.BLOCK, "bed");
    public static final Family<Block> CARPET = new Family<>(BuiltInRegistries.BLOCK, "carpet");
    public static final Family<Block> CONCRETE = new Family<>(BuiltInRegistries.BLOCK, "concrete");
    public static final Family<Block> CONCRETE_POWDER = new Family<>(BuiltInRegistries.BLOCK, "concrete_powder");
    public static final Family<Block> DYED_TERRACOTTA = new Family<>(BuiltInRegistries.BLOCK, "terracotta");
    public static final Family<Block> STAINED_GLASS = new Family<>(BuiltInRegistries.BLOCK, "stained_glass");
    public static final Family<Block> STAINED_GLASS_PANE = new Family<>(BuiltInRegistries.BLOCK, "stained_glass_pane");
    public static final Family<Item> DYE = new Family<>(BuiltInRegistries.ITEM, "dye");

    public static final class Family<T> {
        private final Registry<T> registry;
        private final String suffix;

        Family(Registry<T> registry, String suffix) {
            this.registry = registry;
            this.suffix = suffix;
        }

        public T get(DyeColor color) {
            return registry.get(ResourceLocation.withDefaultNamespace(color.getSerializedName() + "_" + suffix));
        }

        public T white() { return get(DyeColor.WHITE); }
        public T orange() { return get(DyeColor.ORANGE); }
        public T magenta() { return get(DyeColor.MAGENTA); }
        public T lightBlue() { return get(DyeColor.LIGHT_BLUE); }
        public T yellow() { return get(DyeColor.YELLOW); }
        public T lime() { return get(DyeColor.LIME); }
        public T pink() { return get(DyeColor.PINK); }
        public T gray() { return get(DyeColor.GRAY); }
        public T lightGray() { return get(DyeColor.LIGHT_GRAY); }
        public T cyan() { return get(DyeColor.CYAN); }
        public T purple() { return get(DyeColor.PURPLE); }
        public T blue() { return get(DyeColor.BLUE); }
        public T brown() { return get(DyeColor.BROWN); }
        public T green() { return get(DyeColor.GREEN); }
        public T red() { return get(DyeColor.RED); }
        public T black() { return get(DyeColor.BLACK); }
    }
}
