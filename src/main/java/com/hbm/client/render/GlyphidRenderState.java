// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.core.EntityRenderState;
import net.minecraft.resources.ResourceLocation;

public final class GlyphidRenderState extends EntityRenderState {

    public ResourceLocation texture;
    public float bodyYaw;
    public double scale;
    public byte armor;
    public byte subtype;
    public float walkCycle;
    public float swingProgress;
    public float deathAge;
}
