// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import org.jspecify.annotations.Nullable;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface FuseContainerOwner {
    default @Nullable Object hbm$fuseOwner() {
        throw new AbstractMethodError();
    }

    default void hbm$fuseOwner(@Nullable Object owner) {
        throw new AbstractMethodError();
    }

    default void hbm$trackSlots(Object source) {
        throw new AbstractMethodError();
    }
}
