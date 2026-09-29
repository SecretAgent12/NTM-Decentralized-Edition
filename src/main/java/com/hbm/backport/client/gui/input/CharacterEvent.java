// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.input;

import net.minecraft.util.StringUtil;

/** 26.x net.minecraft.client.input.CharacterEvent: one typed code point. */
public record CharacterEvent(int codepoint, int modifiers) implements InputWithModifiers {

    @Override
    public int input() {
        return codepoint;
    }

    public boolean isAllowedChatCharacter() {
        return StringUtil.isAllowedChatCharacter((char) codepoint);
    }

    public String codepointAsString() {
        return Character.toString(codepoint);
    }
}
