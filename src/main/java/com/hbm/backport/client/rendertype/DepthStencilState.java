// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

/**
 * 26.x {@code com.mojang.blaze3d.pipeline.DepthStencilState}: depth test, depth write and depth bias.
 * backport: 1.21.1 disables GL depth testing for {@link CompareOp#ALWAYS_PASS}, which also stops depth
 * writes; the bias becomes a polygon-offset layering shard.
 */
public record DepthStencilState(
        CompareOp depthTest, boolean writeDepth, float depthBiasScaleFactor, float depthBiasConstant) {
    public static final DepthStencilState DEFAULT = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);

    public DepthStencilState(CompareOp depthTest, boolean writeDepth) {
        this(depthTest, writeDepth, 0F, 0F);
    }

    public boolean hasDepthBias() {
        return depthBiasScaleFactor != 0F || depthBiasConstant != 0F;
    }
}
