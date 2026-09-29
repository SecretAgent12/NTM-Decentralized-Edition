// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface PlayerAppearance {

    byte MANLY = 1;
    byte STEALTH = 2;

    default byte hbm$appearance() {

        throw new AssertionError("backport: implemented by mixin");

    }
    default void hbm$setAppearance(byte flags) {

        throw new AssertionError("backport: implemented by mixin");

    }
}
