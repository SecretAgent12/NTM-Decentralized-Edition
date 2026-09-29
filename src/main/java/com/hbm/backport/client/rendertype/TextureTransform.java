// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderStateShard;
import org.joml.Matrix4f;

/**
 * 26.x {@code net.minecraft.client.renderer.rendertype.TextureTransform}: a texture matrix sampled per
 * draw. backport: a 1.21.1 texturing shard setting {@code RenderSystem}'s texture matrix ({@code TextureMat}
 * uniform; the backport entity programs apply it when the pipeline defines APPLY_TEXTURE_MATRIX).
 */
public final class TextureTransform {
    public static final TextureTransform DEFAULT_TEXTURING =
            new TextureTransform(RenderStateShard.DEFAULT_TEXTURING);
    public static final TextureTransform GLINT_TEXTURING = new TextureTransform(RenderStateShard.GLINT_TEXTURING);
    public static final TextureTransform ENTITY_GLINT_TEXTURING =
            new TextureTransform(RenderStateShard.ENTITY_GLINT_TEXTURING);

    private final RenderStateShard.TexturingStateShard shard;

    public TextureTransform(String name, Supplier<Matrix4f> matrix) {
        this(
                new RenderStateShard.TexturingStateShard(
                        name, () -> RenderSystem.setTextureMatrix(matrix.get()), RenderSystem::resetTextureMatrix));
    }

    public TextureTransform(RenderStateShard.TexturingStateShard shard) {
        this.shard = shard;
    }

    public RenderStateShard.TexturingStateShard shard() {
        return shard;
    }
}
