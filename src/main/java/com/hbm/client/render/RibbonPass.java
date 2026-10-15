// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.compat.iris.IrisCompat;
import com.hbm.backport.client.compat.iris.ShadowRenderingState;
import net.neoforged.fml.ModList;

public final class RibbonPass {
    private static final boolean IRIS = ModList.get().isLoaded("iris");

    private RibbonPass() {}

    public static boolean isShadow() {
        return IRIS && IrisShadow.active();
    }

    public static boolean hasShaderPack() {
        return IRIS && IrisShadow.hasShaderPack();
    }

    private static final class IrisShadow {
        private static boolean active() {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
        }

        // backport: through IrisCompat (reflective IrisApi), Iris is no compile dependency of DE
        private static boolean hasShaderPack() {
            return IrisCompat.isShaderPackInUse();
        }
    }
}
