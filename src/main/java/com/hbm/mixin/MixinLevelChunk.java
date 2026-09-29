// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.handler.radiation.RadiationSystemNT;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk extends ChunkAccess {

    public MixinLevelChunk(
            ChunkPos chunkPos,
            UpgradeData upgradeData,
            LevelHeightAccessor levelHeightAccessor,
            net.minecraft.core.Registry<net.minecraft.world.level.biome.Biome> biomeRegistry, // backport: 1.21.1 ChunkAccess ctor
            long inhabitedTime,
            LevelChunkSection @Nullable [] sections,
            @Nullable BlendingData blendingData) {
        super(
                chunkPos,
                upgradeData,
                levelHeightAccessor,
                biomeRegistry,
                inhabitedTime,
                sections,
                blendingData);
    }

    @Inject(
            method =
                    "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
            at = @At("RETURN"))
    private void hbm$carryRadiation(
            ServerLevel level,
            ProtoChunk protoChunk,
            LevelChunk.PostLoadProcessor postLoad,
            CallbackInfo ci) {
        byte[] rad = protoChunk.hbm$getRadiation();
        if (rad != null) hbm$setRadiation(rad);

        long[] index = protoChunk.hbm$coreIndex();
        if (index != null) {
            hbm$setCoreIndex(index, protoChunk.hbm$coreIndexSize());
        }
    }

    // backport: 1.21.1 LevelChunk.setBlockState(BlockPos, BlockState, boolean isMoving)
    @Inject(
            method =
                    "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"))
    private void hbm$radShieldEdit(
            BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        RadiationSystemNT.onBlockStateReplaced((LevelChunk) (Object) this, pos, state);
    }

    /**
     * backport: 26.x ServerLevel.onBlockEntityAdded (MixinServerLevel). In 1.21.1 the equivalent
     * call sites (addAndRegisterBlockEntity while the chunk is in the level, and
     * registerAllBlockEntitiesAfterLevelLoad) both run addGameEventListener for server levels.
     */
    @Inject(
            method =
                    "addGameEventListener(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("TAIL"))
    private void hbm$graphResidentLoad(
            net.minecraft.world.level.block.entity.BlockEntity blockEntity,
            ServerLevel level,
            CallbackInfo ci) {
        if (blockEntity instanceof com.hbm.tileentity.GraphResident)
            com.hbm.tileentity.GraphResident.onLoad(level, blockEntity);
    }
}
