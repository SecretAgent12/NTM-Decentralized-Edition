// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.input;

import org.lwjgl.glfw.GLFW;

/** 26.x net.minecraft.client.input.MouseButtonInfo. */
public record MouseButtonInfo(int button, int modifiers) implements InputWithModifiers {

    @Override
    public int input() {
        return button;
    }

    public boolean isLeft() {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    public boolean isRight() {
        return button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    public boolean isMiddle() {
        return button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
    }
}
