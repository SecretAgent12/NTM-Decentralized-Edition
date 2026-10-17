// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.registration;

import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

public final class OverridePoint<T> {

    private final ResourceLocation id;
    private T value;
    private @Nullable String owner;

    private OverridePoint(ResourceLocation id, T ntm) {
        this.id = id;
        this.value = ntm;
    }

    public static <T> OverridePoint<T> of(ResourceLocation id, T ntm) {
        return new OverridePoint<>(id, ntm);
    }

    public void install(String namespace, T replacement) {
        NeoForgeRegistrar.assertOpen("OverridePoint.install(" + id + ")");
        if (owner != null) {
            throw new IllegalStateException(
                    id
                            + " is already overridden by "
                            + owner
                            + ", so "
                            + namespace
                            + " cannot override it too");
        }
        owner = namespace;
        value = replacement;
    }

    public T get() {
        return value;
    }

    public @Nullable String owner() {
        return owner;
    }

    public ResourceLocation id() {
        return id;
    }
}
