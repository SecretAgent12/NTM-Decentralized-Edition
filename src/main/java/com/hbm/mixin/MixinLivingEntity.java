// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blocks.ClimbBoxes;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.hazard.HazardSystem;
import com.hbm.interfaces.NtmDamageContext;
import com.hbm.items.IAnimatedItem;
import com.hbm.items.ISwingReceiver;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.food.FoodAdditives;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.hbm.platform.Services;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity implements NtmDamageContext {

    @Unique private float hbm$pierceDT;
    @Unique private float hbm$pierceDR;
    @Unique private double hbm$knockbackMultiplier = Double.NaN;
    @Unique private boolean hbm$scaledKnockback;
    @Unique private boolean hbm$ignoreEarlyCancellation;
    @Unique private @Nullable AABB hbm$climbProbed;
    @Unique private int hbm$climbProbeTick;
    @Unique private @Nullable BlockPos hbm$climbBoxAt;

    @Shadow private Optional<BlockPos> lastClimbablePos;

    @Shadow
    private ItemStack getLastHandItem(EquipmentSlot slot) {
        throw new AssertionError();
    }

    // backport: 1.21.1 collectEquipmentChanges() takes no map; the previous main-hand stack is
    // still in lastHandItemStacks at its HEAD (getLastHandItem).
    @Inject(method = "collectEquipmentChanges", at = @At("HEAD"))
    private void hbm$equipAnimatedTool(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        ItemStack current = player.getMainHandItem();
        if (getLastHandItem(EquipmentSlot.MAINHAND).getItem() != current.getItem()
                && current.getItem() instanceof IAnimatedItem item) item.onEquip(player, current);
    }

    @Override
    public float hbm$pierceDT() {
        return hbm$pierceDT;
    }

    @Override
    public float hbm$pierceDR() {
        return hbm$pierceDR;
    }

    @Override
    public void hbm$setPiercing(float dt, float dr) {
        hbm$pierceDT = dt;
        hbm$pierceDR = dr;
    }

    @Override
    public double hbm$knockbackMultiplier() {
        return hbm$knockbackMultiplier;
    }

    @Override
    public void hbm$setKnockbackMultiplier(double multiplier) {
        hbm$knockbackMultiplier = multiplier;
    }

    @Override
    public boolean hbm$ignoreEarlyCancellation() {
        return hbm$ignoreEarlyCancellation;
    }

    @Override
    public void hbm$setIgnoreEarlyCancellation(boolean ignore) {
        hbm$ignoreEarlyCancellation = ignore;
    }

    // backport: 26.x dealDefaultKnockback -> knockback(strength, x, z, source, damage); in 1.21.1
    // the default hit knockback is the knockback(0.4, x, z) call inside hurt(DamageSource, float).
    @WrapOperation(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void hbm$scaledHitKnockback(
            LivingEntity self, double strength, double x, double z, Operation<Void> original) {
        if (Double.isNaN(hbm$knockbackMultiplier)) {
            original.call(self, strength, x, z);
        } else if (hbm$knockbackMultiplier > 0D) {
            boolean previous = hbm$scaledKnockback;
            hbm$scaledKnockback = true;
            try {
                original.call(self, 0.4D * hbm$knockbackMultiplier, x, z);
            } finally {
                hbm$scaledKnockback = previous;
            }
        }
    }

    // backport: 1.21.1 knockback(double, double, double) holds the vertical Math.min(0.4, ...)
    @WrapOperation(
            method = "knockback(DDD)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"))
    private double hbm$scaledHitLift(double limit, double lift, Operation<Double> original) {
        return hbm$scaledKnockback
                ? (lift > 0.2D ? 0.2D * hbm$knockbackMultiplier : lift)
                : original.call(limit, lift);
    }

    @ModifyArg(
            method = "getDamageAfterArmorAbsorb",
            index = 3,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
    private float hbm$piercedVanillaArmor(float armor) {
        return Services.CONFIG.runtime().damageCompatibilityMode()
                ? armor
                : armor * (1F - hbm$pierceDR);
    }

    @WrapOperation(
            method = "getDamageAfterArmorAbsorb",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void hbm$armorWearOutsideSedna(
            LivingEntity self, DamageSource source, float damage, Operation<Void> original) {
        if (Double.isNaN(hbm$knockbackMultiplier)
                || Services.CONFIG.runtime().damageCompatibilityMode()) {
            original.call(self, source, damage);
        }
    }

    @WrapOperation(
            method = "completeUsingItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;"
                                            + "Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack hbm$applyFoodAdditives(
            ItemStack stack, Level level, LivingEntity entity, Operation<ItemStack> original) {
        int additives =
                FoodAdditives.isFood(stack)
                        ? stack.getOrDefault(ModDataComponents.FOOD_ADDITIVES.get(), 0)
                        : 0;
        ItemStack result = original.call(stack, level, entity);
        if (additives != 0) FoodAdditives.apply(entity, additives);
        return result;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void hbm$livingUpdate(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        HazardSystem.tickMobEquipment(self);
        EntityEffectHandler.onUpdate(self);
    }

    @ModifyReturnValue(method = "onClimbable", at = @At("RETURN"))
    private boolean hbm$climbBox(boolean climbing) {
        LivingEntity self = (LivingEntity) (Object) this;
        // backport: 26.x also bails out while gliding through #can_glide_through blocks (vanilla's own
        // onClimbable rule there); 1.21.1 has neither the tag nor that rule.
        if (climbing || self.isSpectator()) {
            return climbing;
        }
        AABB bb = self.getBoundingBox();

        if (bb != hbm$climbProbed || self.tickCount != hbm$climbProbeTick) {
            hbm$climbProbed = bb;
            hbm$climbProbeTick = self.tickCount;
            hbm$climbBoxAt = ClimbBoxes.find(self.level(), bb);
        }
        if (hbm$climbBoxAt == null) return false;
        lastClimbablePos = Optional.of(hbm$climbBoxAt);
        return true;
    }

    // backport: 26.x hurtServer(ServerLevel, DamageSource, float) == 1.21.1 hurt(DamageSource, float)
    @Inject(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$armorCancel(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player)) return;
        if (source.is(DamageTypeTags.IS_FIRE)
                && ArmorSuitEffects.wearsAny(self, ModArmorItem.Suit.ASBESTOS)) {
            cir.setReturnValue(false);
        } else if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                && self.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.NO9.get()
                && damage <= 0.5F) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doHurtEquipment", at = @At("HEAD"))
    private void hbm$drainSuitsOnArmorDamage(
            DamageSource source, float damage, EquipmentSlot[] slots, CallbackInfo ci) {
        if (damage <= 0F) return;
        int wear = (int) Math.max(1.0F, damage / 4.0F);
        for (EquipmentSlot slot : slots)
            ArmorUtil.drainSupplyForWear((LivingEntity) (Object) this, slot, wear);
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    private void hbm$entitySwing(
            InteractionHand hand, boolean sendToSwingingEntity, CallbackInfo ci) {
        if (hand != InteractionHand.MAIN_HAND || !((Object) this instanceof ServerPlayer player))
            return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof ISwingReceiver receiver)
            receiver.onEntitySwing(player, stack);
    }

    // backport: drop(ItemStack, boolean, boolean) is declared on Player in 1.21.1, not LivingEntity;
    // hbm$discardAkimboGhost moved to com.hbm.mixin.backport.MixinsCommonPlayerDrop.

    @WrapOperation(
            method = "checkFallDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;sendParticles"
                                            + "(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int hbm$landingParticlesFollowTheMachine(
            ServerLevel level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            int count,
            double xDist,
            double yDist,
            double zDist,
            double speed,
            Operation<Integer> original,
            @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) BlockState onState) {
        ParticleOptions owned = MultiblockSurface.particleOptions(level, pos, onState, particle);
        return owned == null
                ? 0
                : original.call(level, owned, x, y, z, count, xDist, yDist, zDist, speed);
    }
}
