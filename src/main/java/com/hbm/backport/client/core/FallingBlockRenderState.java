// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.core;

/** 26.x {@code net.minecraft.client.renderer.entity.state.FallingBlockRenderState}. */
public class FallingBlockRenderState extends EntityRenderState {
    public MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
}
