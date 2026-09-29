// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.slf4j.Logger;

public record PaddedSpriteSource(ResourceLocation resourceId, Optional<ResourceLocation> spriteId)
        implements SpriteSource {

    public static final MapCodec<PaddedSpriteSource> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("resource")
                                                    .forGetter(PaddedSpriteSource::resourceId),
                                            ResourceLocation.CODEC
                                                    .optionalFieldOf("sprite")
                                                    .forGetter(PaddedSpriteSource::spriteId))
                                    .apply(instance, PaddedSpriteSource::new));
    private static final Logger LOGGER = LogUtils.getLogger();

    // backport: 1.21.1 sprite sources name a registered SpriteSourceType instead of returning
    // their codec; set by com.hbm.backport.client.blockmodel.BlockModels on
    // RegisterSpriteSourceTypesEvent ("hbm:padded")
    public static SpriteSourceType TYPE;

    private static PaddedSpriteContents load(
            ResourceLocation spriteLocation, ResourceLocation resourceLocation, Resource resource) {
        try {
            ResourceMetadata metadata = resource.metadata();
            if (metadata.getSection(AnimationMetadataSection.SERIALIZER).isPresent()) {
                throw new IOException(
                        "Animated padded atlas sprites are unsupported: " + resourceLocation);
            }
            try (NativeImage source = readImage(resource)) {
                int width = source.getWidth();
                int height = source.getHeight();
                int mipLevels = Minecraft.getInstance().options.mipmapLevels().get();
                int requiredMultiple = mipLevels <= 0 ? 1 : 1 << mipLevels;
                int size = Math.max(width, height);
                int remainder = size % requiredMultiple;
                if (remainder != 0) size += requiredMultiple - remainder;

                NativeImage padded = new NativeImage(NativeImage.Format.RGBA, size, size, false);
                for (int y = 0; y < size; y++) {
                    int sourceY = Math.min(y, height - 1);
                    for (int x = 0; x < size; x++) {
                        padded.setPixelRGBA(
                                x, y, source.getPixelRGBA(Math.min(x, width - 1), sourceY));
                    }
                }

                return new PaddedSpriteContents(
                        spriteLocation,
                        new FrameSize(size, size),
                        padded,
                        metadata,
                        width / (float) size,
                        height / (float) size);
            }
        } catch (RuntimeException | IOException e) {
            LOGGER.error("Unable to load padded sprite {}", resourceLocation, e);
            return null;
        }
    }

    private static NativeImage readImage(Resource resource) throws IOException {
        try (InputStream stream = resource.open()) {
            return NativeImage.read(stream);
        }
    }

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        ResourceLocation resourceLocation = TEXTURE_ID_CONVERTER.idToFile(resourceId);
        Optional<Resource> resource = resourceManager.getResource(resourceLocation);
        if (resource.isEmpty()) {
            LOGGER.warn("Missing padded sprite: {}", resourceLocation);
            return;
        }

        ResourceLocation spriteLocation = spriteId.orElse(resourceId);
        output.add(spriteLocation, loader -> load(spriteLocation, resourceLocation, resource.get()));
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
