// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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

public final class SinglePieceStructure extends JigsawStructure {
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
                                                                            structure.maxHeight))
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

    public SinglePieceStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> pool,
            int heightOffset,
            int minHeight,
            int maxHeight) {

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
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // backport-fix: BF-011 the piece is now positioned eagerly so a flooded footprint can reject the start
        // (the old deferred lambda could only move the piece, never cancel it)
        return super.findGenerationPoint(context)
                .flatMap(
                        stub -> {
                            // backport-fix: BF-011 biome gate first (vanilla checks it after this method), so the
                            // footprint sampling below only runs for starts that can actually spawn
                            if (!validBiomeAt(context, stub.position())) return Optional.empty();
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
                            // backport-fix: BF-011 land structures (factory, crane, planes, forest_chem, ...) may start
                            // in a beach/swamp/plains column next to an ocean, river or pond; averaging OCEAN_FLOOR_WG
                            // then sinks them into the water. Skip starts whose footprint is noticeably flooded.
                            // Water structures (all biomes ocean/beach/river) keep the old behaviour.
                            if (!isWaterStructure() && flooded(context, footprint))
                                return Optional.empty();
                            int y = averageGround(context, footprint) + heightOffset;
                            if (!(context.chunkGenerator() instanceof FlatLevelSource))
                                y = Math.clamp(y, minHeight, maxHeight);
                            piece.move(0, y - footprint.minY(), 0);
                            return Optional.of(
                                    new GenerationStub(
                                            stub.position(), output -> output.addPiece(piece)));
                        });
    }

    private static boolean validBiomeAt(GenerationContext context, BlockPos pos) {
        return context.validBiome()
                .test(
                        context.chunkGenerator()
                                .getBiomeSource()
                                .getNoiseBiome(
                                        QuartPos.fromBlock(pos.getX()),
                                        QuartPos.fromBlock(pos.getY()),
                                        QuartPos.fromBlock(pos.getZ()),
                                        context.randomState().sampler()));
    }

    // backport-fix: BF-011 a structure whose every biome is ocean/beach/river/shore is meant to touch water
    // (aircraft_carrier, oil_rig, lighthouse, beached_patrol)
    private Boolean waterStructure;

    private boolean isWaterStructure() {
        if (waterStructure == null) {
            boolean water = biomes().size() > 0;
            for (Holder<Biome> biome : biomes()) {
                if (!(biome.is(BiomeTags.IS_OCEAN)
                        || biome.is(BiomeTags.IS_BEACH)
                        || biome.is(BiomeTags.IS_RIVER)
                        || biome.is(Biomes.STONY_SHORE))) {
                    water = false;
                    break;
                }
            }
            waterStructure = water;
        }
        return waterStructure;
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

    private static int averageGround(GenerationContext context, BoundingBox footprint) {
        long total = 0;
        for (int z = footprint.minZ(); z <= footprint.maxZ(); z++) {
            for (int x = footprint.minX(); x <= footprint.maxX(); x++) {
                total +=
                        context.chunkGenerator()
                                .getBaseHeight(
                                        x,
                                        z,
                                        Heightmap.Types.OCEAN_FLOOR_WG,
                                        context.heightAccessor(),
                                        context.randomState());
            }
        }
        return (int) (total / ((long) footprint.getXSpan() * footprint.getZSpan()));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.SINGLE_PIECE.get();
    }
}
