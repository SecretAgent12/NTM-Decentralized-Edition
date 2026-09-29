// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.ctm;

import com.hbm.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum CtmEngine {
    CONTINUITY("continuity", "Continuity", "ctm_continuity"),
    FUSION("fusion", "Fusion", "ctm_fusion"),
    CTM_LIB("ctm", "CTM Lib", "ctm_lib"),
    NONE("", "", "");

    private final String mod;
    private final String title;
    private final String pack;

    CtmEngine(String mod, String title, String pack) {
        this.mod = mod;
        this.title = title;
        this.pack = pack;
    }

    public static CtmEngine selected() {
        return Selection.ENGINE;
    }

    public ResourceLocation packId() {
        return ResourceLocation.fromNamespaceAndPath("hbm", pack);
    }

    public String directory() {
        return "resourcepacks/" + packId().getPath();
    }

    public Component title() {
        return Component.translatable("pack.hbm.ctm", title);
    }

    private static final class Selection {
        private static final CtmEngine ENGINE = select();

        private static CtmEngine select() {
            for (CtmEngine engine : values()) {
                // backport: CTM_LIB is the 26.x CTM Lib (io.github.chiselteam.ctm, mod id "ctm"). On
                // 1.21.1 the mod id "ctm" belongs to Chisel's ConnectedTexturesMod, a different API,
                // and the ctm_lib pack's blockstates use the NeoForge 26 custom block model
                // definition "hbm:ctm", which 1.21.1 cannot load. Selecting it would break those
                // blocks' models, so it is never selected here.
                if (engine == CTM_LIB) continue;
                if (engine != NONE && Services.PLATFORM.isModLoaded(engine.mod)) return engine;
            }
            return NONE;
        }
    }
}
