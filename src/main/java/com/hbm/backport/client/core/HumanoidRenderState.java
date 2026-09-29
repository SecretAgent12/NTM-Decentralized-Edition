// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 26.x {@code net.minecraft.client.renderer.entity.state.HumanoidRenderState} (with the 26.x
 * ArmedEntityRenderState fields folded in). 1.21.1 has no humanoid state: {@link #of} extracts one
 * from the entity for code (armor hooks) that is handed a 1.21.1 entity instead.
 */
public class HumanoidRenderState extends LivingEntityRenderState {
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public HumanoidArm attackArm = HumanoidArm.RIGHT;
    public float swimAmount;
    public float attackTime;
    public float speedValue = 1.0F;
    public float maxCrossbowChargeDuration;
    public int ticksUsingItem;
    public InteractionHand useItemHand = InteractionHand.MAIN_HAND;
    public boolean isCrouching;
    public boolean isFallFlying;
    public boolean isVisuallySwimming;
    public boolean isPassenger;
    public boolean isUsingItem;
    public float elytraRotX;
    public float elytraRotY;
    public float elytraRotZ;
    public ItemStack headEquipment = ItemStack.EMPTY;
    public ItemStack chestEquipment = ItemStack.EMPTY;
    public ItemStack legsEquipment = ItemStack.EMPTY;
    public ItemStack feetEquipment = ItemStack.EMPTY;

    public static void extractHumanoid(LivingEntity entity, HumanoidRenderState state, float pt) {
        LivingEntityRenderState.extractLiving(entity, state, pt);
        state.mainArm = entity.getMainArm();
        state.attackArm =
                entity.swingingArm == InteractionHand.MAIN_HAND
                        ? entity.getMainArm()
                        : entity.getMainArm().getOpposite();
        state.swimAmount = entity.getSwimAmount(pt);
        state.attackTime = entity.getAttackAnim(pt);
        state.ticksUsingItem = entity.getTicksUsingItem();
        state.useItemHand = entity.getUsedItemHand();
        state.isCrouching = entity.isCrouching();
        state.isFallFlying = entity.isFallFlying();
        state.isVisuallySwimming = entity.isVisuallySwimming();
        state.isPassenger = entity.isPassenger();
        state.isUsingItem = entity.isUsingItem();
        state.headEquipment = entity.getItemBySlot(EquipmentSlot.HEAD);
        state.chestEquipment = entity.getItemBySlot(EquipmentSlot.CHEST);
        state.legsEquipment = entity.getItemBySlot(EquipmentSlot.LEGS);
        state.feetEquipment = entity.getItemBySlot(EquipmentSlot.FEET);
    }

    /** A fully extracted state for a 1.21.1 entity (base + living + humanoid fields). */
    public static HumanoidRenderState of(LivingEntity entity, float partialTick, int lightCoords) {
        HumanoidRenderState state = new HumanoidRenderState();
        EntityRenderState.extractEntity(entity, state, partialTick, lightCoords);
        extractHumanoid(entity, state, partialTick);
        return state;
    }
}
