// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.environment.Environment;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 500)
public abstract class MixinAbstractFurnaceBlockEntity {
    // backport: 1.21.1 serverTick(Level, BlockPos, BlockState, AbstractFurnaceBlockEntity) calls the
    // instance getBurnDuration(ItemStack) (26.x: getBurnDuration(FuelValues, ItemStack) on a
    // ServerLevel); serverTick only runs on the server, so the level is a ServerLevel
    @WrapOperation(
            method = "serverTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration("
                                            + "Lnet/minecraft/world/item/ItemStack;)I"))
    private static int hbm$burnNeedsAir(
            AbstractFurnaceBlockEntity entity,
            ItemStack fuel,
            Operation<Integer> original,
            Level level,
            BlockPos pos) {
        int burnTime = original.call(entity, fuel);
        return burnTime > 0
                        && !fuel.is(Items.LAVA_BUCKET)
                        && level instanceof ServerLevel server
                        && !Environment.breatheAir(server, pos, 0)
                ? 0
                : burnTime;
    }
}
