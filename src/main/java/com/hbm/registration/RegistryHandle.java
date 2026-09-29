// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public interface RegistryHandle<T> extends Supplier<T>, ItemLike {

    static <T> RegistryHandle<T> of(ResourceLocation id, Supplier<T> supplier) {
        return new RegistryHandle<>() {
            @Override
            public ResourceLocation id() {
                return id;
            }

            @Override
            public T get() {
                return supplier.get();
            }
        };
    }

    static <T> RegistryHandle<T> bound(ResourceLocation id, T value) {
        return new RegistryHandle<>() {
            @Override
            public ResourceLocation id() {
                return id;
            }

            @Override
            public T get() {
                return value;
            }
        };
    }

    ResourceLocation id();

    @Override
    default Item asItem() {
        T value = get();
        if (value instanceof ItemLike like) {
            return like.asItem();
        }
        throw new UnsupportedOperationException(id() + " is not item-like: " + value);
    }
}
