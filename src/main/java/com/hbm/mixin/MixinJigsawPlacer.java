// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.world.gen.nbt.JigsawFreeSpace;
import com.hbm.world.gen.nbt.ParentOverlapPoolElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
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
public abstract class MixinJigsawPlacer {
    @Shadow @Final private List<? super PoolElementStructurePiece> pieces;
    @Unique private MutableObject<VoxelShape> hbm$outerFree;
    @Unique private @Nullable AABB hbm$outerBounds;

    @Unique
    private final IdentityHashMap<MutableObject<VoxelShape>, JigsawFreeSpace> hbm$freeSpace =
            new IdentityHashMap<>();

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
        }
    }

    @Unique
    private JigsawFreeSpace hbm$space(MutableObject<VoxelShape> holder) {
        return hbm$freeSpace.computeIfAbsent(holder, h -> JigsawFreeSpace.of(h.getValue()));
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
            // StructureBlockInfo, the candidate box the 4th BoundingBox (boundingbox3), the child
            // free-space holder the 3rd MutableObject (after the `free` param and the piece-local one).
            // (Mixin's LocalVariableDiscriminator numbers ordinals over the whole frame, params included.)
            @Local(ordinal = 0) StructureTemplate.StructureBlockInfo connector,
            @Local(ordinal = 3) BoundingBox candidate,
            @Local(ordinal = 2) LocalRef<MutableObject<VoxelShape>> childSpace) {
        if (!(source.getElement() instanceof ParentOverlapPoolElement element)
                || !element.allowsParentOverlap(hbm$jigsawName(connector)))
            return !hbm$space(childSpace.get()).contains(candidate);
        if (hbm$outerBounds == null) return true;
        AABB box = proposed.bounds();
        if (box.minX < hbm$outerBounds.minX
                || box.minY < hbm$outerBounds.minY
                || box.minZ < hbm$outerBounds.minZ
                || box.maxX > hbm$outerBounds.maxX
                || box.maxY > hbm$outerBounds.maxY
                || box.maxZ > hbm$outerBounds.maxZ) return true;
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
            @Local(ordinal = 3) BoundingBox candidate,
            @Local(ordinal = 2) LocalRef<MutableObject<VoxelShape>> childSpace) {
        hbm$space(childSpace.get()).subtract(candidate);
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
