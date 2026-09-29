// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

public record OreGenerationProfile(
        ResourceLocation id,
        ResourceLocation dimension,
        ResourceLocation feature,
        List<ResourceLocation> biomes,
        List<ResourceLocation> blocks,
        OreHeightProfile height,
        double attempts,
        int veinSize,
        List<Component> conditions) {
    public static final Codec<OreGenerationProfile> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("id")
                                                    .forGetter(OreGenerationProfile::id),
                                            ResourceLocation.CODEC
                                                    .fieldOf("dimension")
                                                    .forGetter(OreGenerationProfile::dimension),
                                            ResourceLocation.CODEC
                                                    .fieldOf("feature")
                                                    .forGetter(OreGenerationProfile::feature),
                                            ResourceLocation.CODEC
                                                    .listOf()
                                                    .fieldOf("biomes")
                                                    .forGetter(OreGenerationProfile::biomes),
                                            ResourceLocation.CODEC
                                                    .listOf()
                                                    .fieldOf("blocks")
                                                    .forGetter(OreGenerationProfile::blocks),
                                            OreHeightProfile.CODEC
                                                    .fieldOf("height")
                                                    .forGetter(OreGenerationProfile::height),
                                            Codec.DOUBLE
                                                    .fieldOf("attempts")
                                                    .forGetter(OreGenerationProfile::attempts),
                                            Codec.INT
                                                    .fieldOf("vein_size")
                                                    .forGetter(OreGenerationProfile::veinSize),
                                            ComponentSerialization.CODEC
                                                    .listOf()
                                                    .fieldOf("conditions")
                                                    .forGetter(OreGenerationProfile::conditions))
                                    .apply(instance, OreGenerationProfile::new));

    public OreGenerationProfile {
        biomes = List.copyOf(biomes);
        blocks = List.copyOf(blocks);
        conditions = List.copyOf(conditions);
        if (!Double.isFinite(attempts) || attempts < -1 || veinSize < -1)
            throw new IllegalArgumentException();
    }
}
