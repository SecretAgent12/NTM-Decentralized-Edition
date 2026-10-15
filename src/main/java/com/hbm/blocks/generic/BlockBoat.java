// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ISectionGeometry;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import com.hbm.backport.compat.BlockCompat;

public class BlockBoat extends BlockCompat implements ISectionGeometry {

    public static final MapCodec<BlockBoat> CODEC = simpleCodec(BlockBoat::new);

    public BlockBoat(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<BlockBoat> codec() {
        return CODEC;
    }
}
