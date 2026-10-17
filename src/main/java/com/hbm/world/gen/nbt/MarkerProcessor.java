// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.backport.StructureProcessorCompat;
import net.minecraft.world.level.block.state.BlockState;

// backport: an interface extending StructureProcessor in 26.x; 1.21.1's StructureProcessor is a class
public abstract class MarkerProcessor extends StructureProcessorCompat {

    public abstract BlockState marker();
}
