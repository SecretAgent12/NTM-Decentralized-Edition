// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.uninos.graph.EndpointRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface BlockEntityLifecycle {
    default void hbm$declareEndpoint(ServerLevel level, BlockEntity be) {
        EndpointRegistry.declareSelfIfEndpoint(level, be);
    }

    default void hbm$blockEntityLoaded(ServerLevel level, BlockEntity be) {}

    default void hbm$blockEntityRemoved(ServerLevel level, BlockEntity be) {}
}
