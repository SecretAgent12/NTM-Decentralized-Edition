// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.client;

import com.hbm.items.weapon.sedna.AkimboGhost;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import com.hbm.backport.client.itemmodel.ItemDefinitionRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

public final class GunClientExtensions implements IClientItemExtensions {

    public static final GunClientExtensions INSTANCE = new GunClientExtensions();

    public static final EnumProxy<HumanoidModel.ArmPose> AKIMBO_POSE =
            new EnumProxy<>(
                    HumanoidModel.ArmPose.class,
                    // backport: 1.21.1 ArmPose(twoHanded, transformer) has no affectsOffhandPose flag
                    // (26.x passed false; with twoHanded=false 1.21.1 PlayerRenderer keeps the
                    // off-hand pose from getArmPose(OFF_HAND))
                    false,
                    (IArmPoseTransformer) GunClientExtensions::akimboArm);

    private GunClientExtensions() {}

    // backport: 1.21.1 IArmPoseTransformer receives the entity instead of the 26.x render state
    private static void akimboArm(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.yRot = (arm == HumanoidArm.RIGHT ? -0.1F : 0.1F) + model.head.yRot;
        part.xRot = (float) (-Math.PI / 2) + model.head.xRot;
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(
            LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (hand == InteractionHand.OFF_HAND) {
            return AkimboGhost.isGhost(stack) ? AKIMBO_POSE.getValue() : null;
        }

        if (AkimboGhost.isAkimbo(stack)
                && AkimboGhost.isGhost(entity.getItemInHand(InteractionHand.OFF_HAND))) {
            return AKIMBO_POSE.getValue();
        }
        return HumanoidModel.ArmPose.BOW_AND_ARROW;
    }

    // backport: guns draw through the 26.x item definition bridge (ItemModelBootstrap skips items
    // that already have client extensions)
    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return ItemDefinitionRenderer.get();
    }
}
