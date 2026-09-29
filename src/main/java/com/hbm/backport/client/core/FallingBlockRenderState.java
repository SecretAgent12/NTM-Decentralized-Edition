// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

/** 26.x {@code net.minecraft.client.renderer.entity.state.FallingBlockRenderState}. */
public class FallingBlockRenderState extends EntityRenderState {
    public MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
}
