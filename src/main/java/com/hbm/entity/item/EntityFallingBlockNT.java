// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.blocks.machine.ISpotlight;
import com.hbm.entity.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.hbm.backport.compat.FallingBlockEntityCompat;

public class EntityFallingBlockNT extends FallingBlockEntityCompat {

    private static final Logger LOGGER = LogUtils.getLogger();

    public EntityFallingBlockNT(EntityType<? extends EntityFallingBlockNT> type, Level level) {
        super(type, level);
    }

    public static EntityFallingBlockNT fall(ServerLevel level, BlockPos pos, BlockState state) {
        EntityFallingBlockNT entity =
                new EntityFallingBlockNT(ModEntities.FALLING_BLOCK_NT.get(), level);
        entity.blockState =
                state.hasProperty(BlockStateProperties.WATERLOGGED)
                        ? state.setValue(BlockStateProperties.WATERLOGGED, false)
                        : state;
        entity.blocksBuilding = true;
        entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.setStartPos(pos);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    public void tick() {

        if (this.blockState.isAir() || this.blockState.getBlock() instanceof ISpotlight) {

            this.discard();
            return;
        }

        Block block = this.blockState.getBlock();
        this.time++;
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        // backport: 26.x applyEffectsFromBlocks(); 1.21.1 move() already applies inside-block effects
        this.handlePortal();
        if (this.level() instanceof ServerLevel serverLevel
                && (this.isAlive() || this.forceTickAfterTeleportToDuplicate)) {
            BlockPos pos = this.blockPosition();
            boolean isConcrete = this.blockState.getBlock() instanceof ConcretePowderBlock;
            boolean isStuckInWater =
                    isConcrete && this.level().getFluidState(pos).is(FluidTags.WATER);
            double moveVec = this.getDeltaMovement().lengthSqr();
            if (isConcrete && moveVec > 1.0) {
                BlockHitResult clip =
                        this.level()
                                .clip(
                                        new ClipContext(
                                                new Vec3(this.xo, this.yo, this.zo),
                                                this.position(),
                                                ClipContext.Block.COLLIDER,
                                                ClipContext.Fluid.SOURCE_ONLY,
                                                this));
                if (clip.getType() != HitResult.Type.MISS
                        && this.level().getFluidState(clip.getBlockPos()).is(FluidTags.WATER)) {
                    pos = clip.getBlockPos();
                    isStuckInWater = true;
                }
            }

            if (this.time == 1) {
                if (!this.level().getBlockState(pos).is(block)) {
                    this.discard();
                    return;
                }
                this.level().removeBlock(pos, false);
            }

            if (!this.onGround() && !isStuckInWater) {
                if ((this.time > 100
                                && (pos.getY() <= this.level().getMinBuildHeight()
                                        || pos.getY() > (this.level().getMaxBuildHeight() - 1)))
                        || this.time > 600) {
                    if (this.dropItem && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                        this.spawnAtLocation(block);
                    }
                    this.discard();
                }
            } else {
                BlockState currentState = this.level().getBlockState(pos);
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
                if (!currentState.is(Blocks.MOVING_PISTON)) {
                    if (!this.cancelDrop) {
                        boolean mayReplace =
                                currentState.canBeReplaced(
                                        new DirectionalPlaceContext(
                                                this.level(),
                                                pos,
                                                Direction.DOWN,
                                                ItemStack.EMPTY,
                                                Direction.UP));

                        boolean wouldSurvive = this.blockState.canSurvive(this.level(), pos);
                        if (mayReplace && wouldSurvive) {
                            if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                                    && this.level().getFluidState(pos).is(Fluids.WATER)) {
                                this.blockState =
                                        this.blockState.setValue(
                                                BlockStateProperties.WATERLOGGED, true);
                            }

                            if (this.level().setBlock(pos, this.blockState, 3)) {
                                serverLevel
                                        .getChunkSource()
                                        .broadcast(
                                                this,
                                                new ClientboundBlockUpdatePacket(
                                                        pos, this.level().getBlockState(pos)));
                                this.discard();
                                if (block instanceof Fallable fallable) {
                                    fallable.onLand(
                                            this.level(), pos, this.blockState, currentState, this);
                                }

                                if (this.blockData != null && this.blockState.hasBlockEntity()) {
                                    BlockEntity blockEntity = this.level().getBlockEntity(pos);
                                    if (blockEntity != null) {
                                        // backport: 1.21.1 block entities save/load CompoundTags directly
                                        try {
                                            RegistryAccess registryAccess =
                                                    this.level().registryAccess();
                                            CompoundTag merged =
                                                    blockEntity.saveWithoutMetadata(registryAccess);
                                            for (String name : this.blockData.getAllKeys()) {
                                                merged.put(name, this.blockData.get(name).copy());
                                            }
                                            // 26.x loadWithComponents hook (BlockEntityCompat overload) runs
                                            com.hbm.backport.BlockEntityCompat.loadWithComponents(blockEntity, merged);
                                        } catch (Exception exception) {
                                            LOGGER.error(
                                                    "Failed to load block entity from falling block",
                                                    exception);
                                        }
                                        blockEntity.setChanged();
                                    }
                                }
                            } else if (this.dropItem
                                    && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                this.discard();
                                this.callOnBrokenAfterFall(block, pos);
                                this.spawnAtLocation(block);
                            }
                        } else {
                            this.discard();
                            if (this.dropItem
                                    && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                this.callOnBrokenAfterFall(block, pos);
                                this.spawnAtLocation(block);
                            }
                        }
                    } else {
                        this.discard();
                        this.callOnBrokenAfterFall(block, pos);
                    }
                }
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
    }

    /** backport: 26.x air drag hook; 1.21.1 FallingBlockEntity hardcodes 0.98. */
    protected float getAirDrag() {
        return 0.98F;
    }
}
