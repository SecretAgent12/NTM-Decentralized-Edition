// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 26.x net.minecraft.client.renderer.block.BlockStateModelSet (ModelManager#getBlockStateModelSet)
 * over 1.21.1's BlockModelShaper. {@link #current()} replaces
 * {@code Minecraft.getInstance().getModelManager().getBlockStateModelSet()}.
 */
public final class BlockStateModelSet {
    private static final BlockStateModelSet INSTANCE = new BlockStateModelSet();

    private BlockStateModelSet() {}

    public static BlockStateModelSet current() {
        return INSTANCE;
    }

    public BlockStateModel get(BlockState state) {
        return BlockStateModels.get(state);
    }

    public Material.Baked getParticleMaterial(BlockState state) {
        return new Material.Baked(
                Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state));
    }
}
