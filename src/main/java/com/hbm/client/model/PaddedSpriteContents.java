// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

public final class PaddedSpriteContents extends SpriteContents {

    private final float uScale;
    private final float vScale;

    // backport: 1.21.1 SpriteContents takes the resource metadata instead of the 26.x decoded
    // animation/texture/additional sections
    public PaddedSpriteContents(
            ResourceLocation name,
            FrameSize frameSize,
            NativeImage image,
            ResourceMetadata metadata,
            float uScale,
            float vScale) {
        super(name, frameSize, image, metadata);
        this.uScale = uScale;
        this.vScale = vScale;
    }

    public float uScale() {
        return uScale;
    }

    public float vScale() {
        return vScale;
    }
}
