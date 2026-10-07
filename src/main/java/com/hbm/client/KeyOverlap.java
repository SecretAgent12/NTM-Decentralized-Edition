// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class KeyOverlap {

    private KeyOverlap() {}

    public static boolean isDown(KeyMapping mapping) {
        if (mapping.isUnbound()) return false;
        Minecraft mc = Minecraft.getInstance();

        if (mc.screen != null) return false;

        // backport-fix: BF-071 the key the player bound, not the default one (rebinding did nothing)
        InputConstants.Key key = mapping.getKey();
        Window window = mc.getWindow();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window.getWindow(), key.getValue());

            default -> mapping.isDown();
        };
    }
}
