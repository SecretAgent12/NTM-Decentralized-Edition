// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity;

import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface GraphResident {

    static void attach(ServerLevel server, BlockEntity be) {
        LevelNodeGraph.invalidateTransportAround(server, be.getBlockPos());
        // backport-fix: BF-087 also the cell of the block entity itself (its own self-endpoint face):
        // a battery socket that had been probed "absent" while unloading stayed out of its network
        LevelNodeGraph.invalidateNodeAt(server, be.getBlockPos());
        be.getBlockState().getBlock().hbm$declareEndpoint(server, be);
    }

    default void onGraphLoad(ServerLevel server) {}

    default void onGraphDetach(ServerLevel server) {}
}
