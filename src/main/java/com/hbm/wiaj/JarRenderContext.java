// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj;

import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.itemmodel.ItemModelResolver;

public record JarRenderContext(WorldInAJar world, PoseStack pose, SubmitNodeCollector collector) {
    public ItemModelResolver itemModelResolver() {
        // backport: 1.21.1 Minecraft has no item model resolver; the itemmodel shim holds the instance
        return ItemModelResolver.get();
    }
}
