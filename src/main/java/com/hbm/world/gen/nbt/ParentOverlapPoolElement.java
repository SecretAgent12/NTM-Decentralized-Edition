// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.nbt;

import com.hbm.world.structure.HbmStructureTypes;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class ParentOverlapPoolElement extends SinglePoolElement {
    public static final MapCodec<ParentOverlapPoolElement> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            ParentOverlapPoolElement
                                                    .<ParentOverlapPoolElement>templateCodec(),
                                            ParentOverlapPoolElement
                                                    .<ParentOverlapPoolElement>processorsCodec(),
                                            ParentOverlapPoolElement
                                                    .<ParentOverlapPoolElement>projectionCodec(),
                                            ParentOverlapPoolElement
                                                    .<ParentOverlapPoolElement>
                                                            overrideLiquidSettingsCodec(),
                                            ResourceLocation.CODEC
                                                    .fieldOf("parent_overlap_connector")
                                                    .forGetter(
                                                            element ->
                                                                    element.parentOverlapConnector))
                                    .apply(instance, ParentOverlapPoolElement::new));

    private final ResourceLocation parentOverlapConnector;

    private ParentOverlapPoolElement(
            Either<ResourceLocation, StructureTemplate> template,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection,
            Optional<LiquidSettings> liquidSettings,
            ResourceLocation parentOverlapConnector) {
        super(template, processors, projection, liquidSettings);
        this.parentOverlapConnector = parentOverlapConnector;
    }

    public static Function<StructureTemplatePool.Projection, ParentOverlapPoolElement> single(
            ResourceLocation template,
            Holder<StructureProcessorList> processors,
            ResourceLocation parentOverlapConnector) {
        return projection ->
                new ParentOverlapPoolElement(
                        Either.left(template),
                        processors,
                        projection,
                        Optional.empty(),
                        parentOverlapConnector);
    }

    public boolean allowsParentOverlap(ResourceLocation connector) {
        return parentOverlapConnector.equals(connector);
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return HbmStructureTypes.PARENT_OVERLAP_POOL_ELEMENT.get();
    }
}
