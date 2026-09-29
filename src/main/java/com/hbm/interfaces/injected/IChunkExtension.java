// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import org.jspecify.annotations.Nullable;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface IChunkExtension {

    String RADIATION_NBT_KEY = "hbm_rad";

    String CORE_INDEX_NBT_KEY = "hbm_core_index";

    default byte @Nullable [] hbm$getRadiation() {

        throw new AssertionError("backport: implemented by mixin");

    }
    default void hbm$setRadiation(byte @Nullable [] bytes) {

        throw new AssertionError("backport: implemented by mixin");

    }
    default long @Nullable [] hbm$coreIndex() {

        throw new AssertionError("backport: implemented by mixin");

    }
    default int hbm$coreIndexSize() {

        throw new AssertionError("backport: implemented by mixin");

    }
    default void hbm$setCoreIndex(long @Nullable [] entries, int size) {

        throw new AssertionError("backport: implemented by mixin");

    }
}
