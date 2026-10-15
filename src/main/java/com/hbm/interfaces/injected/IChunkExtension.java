// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface IChunkExtension {

    String RADIATION_NBT_KEY = "hbm_rad";

    String CORE_INDEX_NBT_KEY = "hbm_core_index";

    String PENDING_FALLERS_NBT_KEY = "hbm_pending_fallers";
    String DUCT_FLUIDS_NBT_KEY = "hbm_duct_fluids";

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
    default long @Nullable [] hbm$pendingFallerPositions() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default BlockState @Nullable [] hbm$pendingFallerStates() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default int hbm$pendingFallerSize() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default void hbm$setPendingFallers(
            long @Nullable [] positions, BlockState @Nullable [] states, int size) {
        throw new AssertionError("backport: implemented by mixin");
    }
    default @Nullable Long2ObjectOpenHashMap<String> hbm$ductFluids() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default void hbm$setDuctFluids(@Nullable Long2ObjectOpenHashMap<String> fluids) {
        throw new AssertionError("backport: implemented by mixin");
    }
}
