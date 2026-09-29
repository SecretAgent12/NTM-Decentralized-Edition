// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.input;

/** 26.x net.minecraft.client.input.MouseButtonEvent: a click at a GUI position. */
public record MouseButtonEvent(double x, double y, MouseButtonInfo buttonInfo) implements InputWithModifiers {

    public MouseButtonEvent(double x, double y, int button) {
        this(x, y, new MouseButtonInfo(button, InputWithModifiers.currentModifiers()));
    }

    public int button() {
        return buttonInfo.button();
    }

    @Override
    public int input() {
        return buttonInfo.button();
    }

    @Override
    public int modifiers() {
        return buttonInfo.modifiers();
    }

    public boolean isLeft() {
        return buttonInfo.isLeft();
    }

    public boolean isRight() {
        return buttonInfo.isRight();
    }

    public boolean isMiddle() {
        return buttonInfo.isMiddle();
    }
}
