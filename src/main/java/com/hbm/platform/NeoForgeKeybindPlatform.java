// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class NeoForgeKeybindPlatform {
    public static boolean sameChord(KeyMapping first, KeyMapping second) {
        return !first.isUnbound()
                && first.getKey().equals(second.getKey())
                && first.getKeyModifier() == second.getKeyModifier();
    }

    public static boolean modifierMatches(KeyMapping mapping, int modifiers) {
        int mask =
                switch (mapping.getKeyModifier()) {
                    case NONE -> 0;
                    case SHIFT -> GLFW.GLFW_MOD_SHIFT;
                    // backport: 1.21.1 KeyModifier has no CONTROL_OR_COMMAND; its CONTROL is that one
                    // (Screen.hasControlDown(): Command on macOS)
                    case CONTROL -> Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
                    case ALT -> GLFW.GLFW_MOD_ALT;
                };
        return (modifiers & mask) == mask;
    }
}
