// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import net.minecraft.world.level.block.entity.BlockEntity;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface MovedBlockEntityData {

    default void hbm$captureFrom(BlockEntity source) {

        throw new AssertionError("backport: implemented by mixin");

    }
    default void hbm$restoreCarried() {

        throw new AssertionError("backport: implemented by mixin");

    }
}
