// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.core;

import net.minecraft.resources.ResourceLocation;

/**
 * 26.x {@code net.minecraft.core.ClientAsset}: an asset id and the texture file it names. 1.21.1
 * PlayerSkin takes the texture location itself ({@link ResourceTexture#texturePath()}).
 */
public final class ClientAsset {
    private ClientAsset() {}

    public record ResourceTexture(ResourceLocation id, ResourceLocation texturePath) {
        public ResourceTexture(ResourceLocation id) {
            this(id, id.withPath(path -> "textures/" + path + ".png"));
        }
    }
}
