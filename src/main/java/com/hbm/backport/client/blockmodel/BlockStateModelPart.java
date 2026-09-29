// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import java.util.List;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** 26.x net.minecraft.client.renderer.block.dispatch.BlockStateModelPart. */
public interface BlockStateModelPart {

    List<BakedQuad> getQuads(@Nullable Direction direction);

    boolean useAmbientOcclusion();

    Material.Baked particleMaterial();

    int materialFlags();
}
