// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.world.gen.WorldgenHeight;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public final class SinglePieceStructure extends JigsawStructure implements ColumnBiome.SelfTested {
    public static final MapCodec<SinglePieceStructure> CODEC =
            RecordCodecBuilder.<SinglePieceStructure>mapCodec(
                            instance ->
                                    instance.group(
                                                    settingsCodec(instance),
                                                    StructureTemplatePool.CODEC
                                                            .fieldOf("start_pool")
                                                            .forGetter(
                                                                    SinglePieceStructure
                                                                            ::getStartPool),
                                                    Codec.INT
                                                            .fieldOf("height_offset")
                                                            .forGetter(
                                                                    structure ->
                                                                            structure.heightOffset),
                                                    Codec.INT
                                                            .optionalFieldOf("min_height", 1)
                                                            .forGetter(
                                                                    structure ->
                                                                            structure.minHeight),
                                                    Codec.INT
                                                            .optionalFieldOf("max_height", 128)
                                                            .forGetter(
                                                                    structure ->
                                                                            structure.maxHeight),
                                                    Codec.BOOL
                                                            .optionalFieldOf("aquatic", false)
                                                            .forGetter(
                                                                    structure -> structure.aquatic))
                                            .apply(instance, SinglePieceStructure::new))
                    .validate(
                            structure ->
                                    structure.minHeight <= structure.maxHeight
                                            ? DataResult.success(structure)
                                            : DataResult.error(
                                                    () -> "Inverted single-piece height band"));

    // backport: 26.x JigsawStructure.getStartPool(); 1.21.1 keeps the start pool private
    private final Holder<StructureTemplatePool> startPool;

    public Holder<StructureTemplatePool> getStartPool() {
        return startPool;
    }

    private final int heightOffset;
    private final int minHeight;
    private final int maxHeight;
    private final boolean aquatic;

    public SinglePieceStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> pool,
            int heightOffset,
            int minHeight,
            int maxHeight,
            boolean aquatic) {
        super(
                settings,
                pool,
                Optional.empty(),
                1,
                ConstantHeight.of(VerticalAnchor.absolute(heightOffset + 1)),
                false,
                Optional.of(Heightmap.Types.OCEAN_FLOOR_WG),
                128, // backport: 1.21.1 takes the plain max distance
                List.of(),
                DEFAULT_DIMENSION_PADDING,
                DEFAULT_LIQUID_SETTINGS);
        this.startPool = pool;
        this.heightOffset = heightOffset;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.aquatic = aquatic;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return super.findGenerationPoint(context)
                .filter(stub -> ColumnBiome.valid(stub, context))
                .flatMap(
                        stub -> {
                            var pieces = stub.getPiecesBuilder().build().pieces();
                            if (pieces.size() != 1)
                                throw new IllegalStateException(
                                        "Single-piece pool "
                                                + getStartPool()
                                                + " produced "
                                                + pieces.size()
                                                + " pieces");
                            PoolElementStructurePiece piece =
                                    (PoolElementStructurePiece) pieces.getFirst();
                            BoundingBox footprint = piece.getBoundingBox();
                            if (!aquatic
                                    && getLowestY(
                                                    context,
                                                    footprint.minX(),
                                                    footprint.minZ(),
                                                    footprint.getXSpan() - 1,
                                                    footprint.getZSpan() - 1)
                                            < context.chunkGenerator().getSeaLevel())
                                return Optional.empty();
                            // backport-fix: BF-011 land structures (factory, crane, planes, forest_chem, ...) may
                            // also start next to a river or pond above sea level; averaging OCEAN_FLOOR_WG then
                            // sinks them into the water. Skip starts whose footprint is noticeably flooded.
                            // (1.0.3's ColumnBiome gate and aquatic flag replace the BF-011 biome gate and
                            // water-structure heuristic.)
                            if (!aquatic && flooded(context, footprint)) return Optional.empty();
                            return Optional.of(
                                    new GenerationStub(
                                            stub.position(),
                                            output -> {
                                                int y =
                                                        WorldgenHeight.averageBaseHeight(
                                                                        context,
                                                                        piece.getBoundingBox())
                                                                + heightOffset;
                                                if (!(context.chunkGenerator()
                                                        instanceof FlatLevelSource))
                                                    y = Math.clamp(y, minHeight, maxHeight);
                                                piece.move(0, y - piece.getBoundingBox().minY(), 0);
                                                output.addPiece(piece);
                                            }));
                        });
    }

    // backport-fix: BF-011 a footprint counts as flooded when more than 1/8 of its columns (sampled on a 2-block grid)
    // have water at least 2 deep on top of the ground; shallow swamp puddles are tolerated
    private static boolean flooded(GenerationContext context, BoundingBox footprint) {
        int samples = 0;
        int wet = 0;
        for (int z = footprint.minZ(); z <= footprint.maxZ(); z += 2) {
            for (int x = footprint.minX(); x <= footprint.maxX(); x += 2) {
                int floor =
                        context.chunkGenerator()
                                .getBaseHeight(
                                        x,
                                        z,
                                        Heightmap.Types.OCEAN_FLOOR_WG,
                                        context.heightAccessor(),
                                        context.randomState());
                int surface =
                        context.chunkGenerator()
                                .getBaseHeight(
                                        x,
                                        z,
                                        Heightmap.Types.WORLD_SURFACE_WG,
                                        context.heightAccessor(),
                                        context.randomState());
                samples++;
                if (surface - floor >= 2) wet++;
            }
        }
        return wet * 8 > samples;
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.SINGLE_PIECE.get();
    }
}
