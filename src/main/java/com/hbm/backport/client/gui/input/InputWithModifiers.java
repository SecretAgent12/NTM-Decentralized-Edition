// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.input;

import org.lwjgl.glfw.GLFW;

/** 26.x net.minecraft.client.input.InputWithModifiers: an input code plus its GLFW modifier bits. */
public interface InputWithModifiers {

    int input();

    int modifiers();

    default boolean hasShiftDown() {
        return (modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    default boolean hasControlDown() {
        // backport: unverified: 26.x treats CMD as control on macOS (Screen.hasControlDown in 1.21.1 does)
        return (modifiers() & (net.minecraft.client.Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL)) != 0;
    }

    default boolean hasAltDown() {
        return (modifiers() & GLFW.GLFW_MOD_ALT) != 0;
    }

    default boolean hasControlDownWithQuirk() {
        return hasControlDown();
    }

    /** GLFW modifier bits from the live key state (1.21.1 mouse hooks do not receive them). */
    static int currentModifiers() {
        int mods = 0;
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) mods |= GLFW.GLFW_MOD_SHIFT;
        if (net.minecraft.client.gui.screens.Screen.hasAltDown()) mods |= GLFW.GLFW_MOD_ALT;
        if (net.minecraft.client.gui.screens.Screen.hasControlDown())
            mods |= net.minecraft.client.Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
        return mods;
    }
}
