// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.world.gen.nbt.JigsawFreeSpace;
import com.hbm.world.gen.nbt.LegacyStartPoolElement;
import com.hbm.world.gen.nbt.ParentOverlapPoolElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public abstract class MixinPlacer {
    @Shadow @Final private int maxDepth;
    @Shadow @Final private List<? super PoolElementStructurePiece> pieces;
    @Unique private MutableObject<VoxelShape> hbm$outerFree;
    @Unique private @Nullable AABB hbm$outerBounds;

    @Unique
    private final IdentityHashMap<MutableObject<VoxelShape>, JigsawFreeSpace> hbm$freeSpace =
            new IdentityHashMap<>();

    @Unique private @Nullable LegacyStartPoolElement hbm$legacy;
    @Unique private BlockPos hbm$start = BlockPos.ZERO;
    @Unique private boolean hbm$fallbacksOnly;
    @Unique private int hbm$templateLists;
    @Unique private int hbm$mainCandidates;
    @Unique private int hbm$candidate;

    @Unique
    private final IdentityHashMap<BoundingBox, BlockPos> hbm$incoming = new IdentityHashMap<>();

    @Inject(method = "tryPlacingChildren", at = @At("HEAD"))
    private void hbm$retainOuterSpace(
            PoolElementStructurePiece source,
            MutableObject<VoxelShape> free,
            int depth,
            boolean expansion,
            LevelHeightAccessor height,
            RandomState random,
            PoolAliasLookup aliases,
            LiquidSettings liquid,
            CallbackInfo ci) {
        if (depth == 0) {
            hbm$outerFree = free;
            hbm$outerBounds = free.getValue().isEmpty() ? null : free.getValue().bounds();
            hbm$legacy = source.getElement() instanceof LegacyStartPoolElement start ? start : null;
            hbm$start = source.getPosition();
        }
        if (hbm$legacy != null) {
            BlockPos center = source.getBoundingBox().getCenter();
            int distance =
                    Math.max(
                            Math.abs(center.getX() - hbm$start.getX()),
                            Math.abs(center.getZ() - hbm$start.getZ()));
            hbm$fallbacksOnly =
                    pieces.size() >= hbm$legacy.sizeLimit() || distance >= hbm$legacy.rangeLimit();
        }
    }

    @WrapOperation(
            method = "tryPlacingChildren",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;getFallback()Lnet/minecraft/core/Holder;"))
    private Holder<StructureTemplatePool> hbm$beginConnector(
            StructureTemplatePool pool, Operation<Holder<StructureTemplatePool>> original) {
        hbm$templateLists = 0;
        hbm$mainCandidates = 0;
        hbm$candidate = 0;
        return original.call(pool);
    }

    @WrapOperation(
            method = "tryPlacingChildren",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;getShuffledTemplates(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private List<StructurePoolElement> hbm$candidateLists(
            StructureTemplatePool pool,
            RandomSource random,
            Operation<List<StructurePoolElement>> original,
            @Local(argsOnly = true) int depth) {
        boolean main = hbm$templateLists++ == 0 && depth != maxDepth;
        if (!main) return original.call(pool, random);
        if (hbm$legacy != null && hbm$fallbacksOnly) return List.of();
        List<StructurePoolElement> candidates = original.call(pool, random);
        hbm$mainCandidates = candidates.size();
        return candidates;
    }

    @WrapOperation(
            method = "tryPlacingChildren",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/Rotation;getShuffled(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private List<Rotation> hbm$nextCandidate(
            RandomSource random, Operation<List<Rotation>> original) {
        hbm$candidate++;
        return original.call(random);
    }

    @Unique
    private JigsawFreeSpace hbm$space(MutableObject<VoxelShape> holder) {
        return hbm$freeSpace.computeIfAbsent(holder, h -> JigsawFreeSpace.of(h.getValue()));
    }

    @Unique
    private boolean hbm$outsideOuterBounds(AABB box) {
        return box.minX < hbm$outerBounds.minX
                || box.minY < hbm$outerBounds.minY
                || box.minZ < hbm$outerBounds.minZ
                || box.maxX > hbm$outerBounds.maxX
                || box.maxY > hbm$outerBounds.maxY
                || box.maxZ > hbm$outerBounds.maxZ;
    }

    @WrapOperation(
            method = "tryPlacingChildren",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/phys/shapes/Shapes;joinIsNotEmpty(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z"))
    private boolean hbm$allowDeclaredParentOverlap(
            VoxelShape free,
            VoxelShape proposed,
            BooleanOp operation,
            Operation<Boolean> original,
            @Local(argsOnly = true) PoolElementStructurePiece source,
            // backport: 1.21.1 has no local names at runtime and no JigsawBlockInfo; the locals are
            // taken by type + ordinal from the 1.21.1 body: the source jigsaw is the first
            // StructureBlockInfo, the attachment point the 3rd BlockPos (blockpos2 = source jigsaw
            // pos + facing), the candidate box the 4th BoundingBox (boundingbox3), the child
            // free-space holder the 3rd MutableObject (after the `free` param and the piece-local one).
            // (Mixin's LocalVariableDiscriminator numbers ordinals over the whole frame, params included.)
            @Local(ordinal = 0) StructureTemplate.StructureBlockInfo connector,
            @Local(ordinal = 2) BlockPos attachment,
            @Local(ordinal = 3) BoundingBox candidate,
            @Local(ordinal = 2) LocalRef<MutableObject<VoxelShape>> childSpace) {
        if (hbm$legacy != null && hbm$candidate > hbm$mainCandidates) {
            if (hbm$outerBounds != null && hbm$outsideOuterBounds(proposed.bounds())) return true;
            if (hbm$fallbacksOnly)
                return connector.pos().equals(hbm$incoming.get(source.getBoundingBox()));
            for (Object value : pieces) {
                PoolElementStructurePiece placed = (PoolElementStructurePiece) value;
                if (placed != source && placed.getBoundingBox().isInside(attachment)) return true;
            }
            return false;
        }
        if (!(source.getElement() instanceof ParentOverlapPoolElement element)
                || !element.allowsParentOverlap(hbm$jigsawName(connector)))
            return !hbm$space(childSpace.get()).contains(candidate);
        if (hbm$outerBounds == null) return true;
        if (hbm$outsideOuterBounds(proposed.bounds())) return true;
        for (Object value : pieces) {
            PoolElementStructurePiece placed = (PoolElementStructurePiece) value;
            if (placed != source && placed.getBoundingBox().intersects(candidate)) return true;
        }
        childSpace.set(hbm$outerFree);
        return false;
    }

    @WrapOperation(
            method = "tryPlacingChildren",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/phys/shapes/Shapes;joinUnoptimized(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape hbm$subtractPlaced(
            VoxelShape free,
            VoxelShape placed,
            BooleanOp operation,
            Operation<VoxelShape> original,
            @Local(ordinal = 2) BlockPos attachment,
            @Local(ordinal = 3) BoundingBox candidate,
            @Local(ordinal = 2) LocalRef<MutableObject<VoxelShape>> childSpace) {
        hbm$space(childSpace.get()).subtract(candidate);
        if (hbm$legacy != null) hbm$incoming.put(candidate, attachment);
        return free;
    }

    /** 26.x JigsawBlockInfo.name(); 1.21.1 keeps the jigsaw name in the block info's nbt. */
    @Unique
    private static net.minecraft.resources.@Nullable ResourceLocation hbm$jigsawName(
            StructureTemplate.StructureBlockInfo info) {
        return info.nbt() == null
                ? null
                : net.minecraft.resources.ResourceLocation.tryParse(info.nbt().getString("name"));
    }
}
