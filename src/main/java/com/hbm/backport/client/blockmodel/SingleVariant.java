// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.blockmodel;

import java.util.List;
import net.minecraft.util.RandomSource;

/** 26.x net.minecraft.client.renderer.block.dispatch.SingleVariant: always the same part. */
public record SingleVariant(BlockStateModelPart model) implements BlockStateModel {

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(model);
    }

    @Override
    public Material.Baked particleMaterial() {
        return model.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return model.materialFlags();
    }
}
