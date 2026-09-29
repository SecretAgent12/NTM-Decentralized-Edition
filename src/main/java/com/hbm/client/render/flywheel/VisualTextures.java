// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.platform.Services;
import com.mojang.blaze3d.platform.NativeImage;
import dev.engine_room.flywheel.api.backend.BackendManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public final class VisualTextures {
    private static final Map<ResourceLocation, Boolean> TRANSLUCENT = new ConcurrentHashMap<>();
    private static final boolean SODIUM = Services.PLATFORM.isModLoaded("sodium");
    private static final Set<TextureAtlasSprite> ANIMATED = ConcurrentHashMap.newKeySet();

    private VisualTextures() {}

    static void animated(TextureAtlasSprite sprite) {
        // backport: 1.21.1 SpriteContents has no isAnimated(); more than one unique frame means animated
        if (SODIUM && sprite.contents().getUniqueFrames().count() > 1) ANIMATED.add(sprite);
    }

    public static void activateSprites() {
        if (SODIUM && BackendManager.isBackendOn()) SodiumSprites.activate();
    }

    public static boolean translucentTexture(ResourceLocation texture) {
        return TRANSLUCENT.computeIfAbsent(
                texture,
                id -> {
                    if (id.equals(TextureAtlas.LOCATION_BLOCKS)) return false;
                    try (var stream =
                                    Minecraft.getInstance()
                                            .getResourceManager()
                                            .getResourceOrThrow(id)
                                            .open();
                            var image = NativeImage.read(stream)) {
                        for (int y = 0; y < image.getHeight(); y++) {
                            for (int x = 0; x < image.getWidth(); x++) {
                                int alpha = image.getPixelRGBA(x, y) >>> 24; // backport: ABGR in 1.21.1, alpha still on top
                                if (alpha != 0 && alpha != 255) return true;
                            }
                        }
                        return false;
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                });
    }

    static void reload() {
        TRANSLUCENT.clear();
        ANIMATED.clear();
    }

    // backport: Sodium's API (SpriteUtil) is not on the 1.21.1 compile classpath; Sodium 0.6 for 1.21.1 has the same
    // net.caffeinemc.mods.sodium.api.texture.SpriteUtil.INSTANCE.markSpriteActive(TextureAtlasSprite), reached reflectively.
    private static final class SodiumSprites {
        private static final java.lang.invoke.MethodHandle MARK = find();

        private static java.lang.invoke.MethodHandle find() {
            try {
                Class<?> api = Class.forName("net.caffeinemc.mods.sodium.api.texture.SpriteUtil");
                Object instance = api.getField("INSTANCE").get(null);
                return java.lang.invoke.MethodHandles.publicLookup()
                        .findVirtual(api, "markSpriteActive",
                                java.lang.invoke.MethodType.methodType(void.class, TextureAtlasSprite.class))
                        .bindTo(instance);
            } catch (ReflectiveOperationException | LinkageError error) {
                return null;
            }
        }

        static void activate() {
            if (MARK == null) return;
            for (var sprite : ANIMATED) {
                try {
                    MARK.invoke(sprite);
                } catch (Throwable error) {
                    return;
                }
            }
        }
    }
}
