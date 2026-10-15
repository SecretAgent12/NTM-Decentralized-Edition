// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;

public interface CapabilityDependentShape {
    static BlockBehaviour.Properties deferShapeToFullChunk(BlockBehaviour.Properties props) {
        // backport: 1.21.1 Properties.hasPostProcess(StatePredicate) marks every placed position for the
        // full-chunk shape update, which is what 26.x postProcess((state, level, pos) -> pos) does
        return props.hasPostProcess((state, level, pos) -> true);
    }

    interface Endpoint extends CapabilityDependentShape {}
}
