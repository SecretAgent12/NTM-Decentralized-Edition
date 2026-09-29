// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.client.model.SectionGeometryIndex;
import com.hbm.client.render.MultiblockOutline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface IClientLevelExtension extends IClientCoreHint {
    default SectionGeometryIndex hbm$sectionGeometryIndex() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default MultiblockOutline hbm$multiblockOutline() {

        throw new AssertionError("backport: implemented by mixin");

    }
    default @Nullable BlockEntity hbm$retainedBlockEntity(BlockPos pos) {

        throw new AssertionError("backport: implemented by mixin");

    }
}
