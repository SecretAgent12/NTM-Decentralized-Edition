// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import net.minecraft.resources.ResourceLocation;

/** 26.x net.minecraft.client.resources.model.ResolvableModel. */
public interface ResolvableModel {
    void resolveDependencies(Resolver resolver);

    interface Resolver {
        void markDependency(ResourceLocation id);
    }
}
