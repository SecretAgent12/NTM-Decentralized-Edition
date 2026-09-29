// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.input;

import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/** 26.x net.minecraft.client.input.KeyEvent: a key press/release with scancode and modifiers. */
public record KeyEvent(int key, int scancode, int modifiers) implements InputWithModifiers {

    @Override
    public int input() {
        return key;
    }

    public boolean isEscape() {
        return key == GLFW.GLFW_KEY_ESCAPE;
    }

    public boolean isConfirmation() {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    public boolean isSelection() {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    public boolean isCycleFocus() {
        return key == GLFW.GLFW_KEY_TAB;
    }

    public boolean isLeft() {
        return key == GLFW.GLFW_KEY_LEFT;
    }

    public boolean isRight() {
        return key == GLFW.GLFW_KEY_RIGHT;
    }

    public boolean isUp() {
        return key == GLFW.GLFW_KEY_UP;
    }

    public boolean isDown() {
        return key == GLFW.GLFW_KEY_DOWN;
    }

    /** 0-9 for the number row / keypad digits, -1 otherwise. */
    public int getDigit() {
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return key - GLFW.GLFW_KEY_0;
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) return key - GLFW.GLFW_KEY_KP_0;
        return -1;
    }

    public boolean isCopy() {
        return Screen.isCopy(key);
    }

    public boolean isCut() {
        return Screen.isCut(key);
    }

    public boolean isPaste() {
        return Screen.isPaste(key);
    }

    public boolean isSelectAll() {
        return Screen.isSelectAll(key);
    }
}
