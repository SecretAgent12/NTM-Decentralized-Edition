// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.flywheel;

import com.hbm.backport.client.core.LivingEntityRenderState;

/** backport: 26.x net.minecraft.client.renderer.entity.state.CreeperRenderState (fields the tree reads). */
public class CreeperRenderState extends LivingEntityRenderState {
    public float swelling;
    public boolean isPowered;
}
