// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create;

import com.hbm.registration.Reg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;

/**
 * backport-fix: BF-016 Create's fluid pipes look up the block entity of the next cell first and
 * ask for the fluid capability only through it. NTM multiblock cells (ports, tanks' sides) have no
 * block entity: their fluid capability is a block capability that resolves the core, so pipes of
 * NTM, Mekanism and AE2 see them and Create's don't. The compat mixins
 * (mixin/compat/createfluid) ask this first and then go to the block capability directly.
 */
public final class CreateMultiblockCells {

    private CreateMultiblockCells() {}

    /** An NTM block without a block entity at {@code pos}: a multiblock cell Create would skip. */
    public static boolean isNtmCell(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) != null) return false;
        String namespace =
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getNamespace();
        return !namespace.isEmpty() && Reg.isNtmNamespace(namespace);
    }
}
