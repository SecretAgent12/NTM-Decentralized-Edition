// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.config.VersatileConfig;
import com.hbm.items.ModDataComponents;
import com.hbm.main.Polaroid;
import com.hbm.util.TickPhase;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.item.food.Consumable;
import com.hbm.backport.item.food.Consumables;
import com.hbm.backport.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.ItemCompat;

public class ItemCanteen extends ItemCompat {

    public static final int COOLDOWN_SECONDS = 180;
    public static final Consumable CONSUMABLE =
            Consumables.defaultDrink().consumeSeconds(0.5F).build();

    public ItemCanteen(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        int remaining = stack.getOrDefault(ModDataComponents.CANTEEN_COOLDOWN.get(), 0);
        if (remaining > 0 && TickPhase.every(entity, SharedConstants.TICKS_PER_SECOND)) {
            stack.set(ModDataComponents.CANTEEN_COOLDOWN.get(), remaining - 1);
        }
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getOrDefault(ModDataComponents.CANTEEN_COOLDOWN.get(), 0)
                        != 0
                || VersatileConfig.hasPotionSickness(player)) {
            return InteractionResult.PASS;
        }
        return super.backport$use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        CONSUMABLE.emitParticlesAndSounds(entity.getRandom(), entity, stack, 16);

        stack.set(ModDataComponents.CANTEEN_COOLDOWN.get(), COOLDOWN_SECONDS);
        entity.addEffect(
                new MobEffectInstance(MobEffects.CONFUSION, 10 * SharedConstants.TICKS_PER_SECOND, 0));
        entity.addEffect(
                new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST, 30 * SharedConstants.TICKS_PER_SECOND, 2));
        VersatileConfig.applyPotionSickness(entity, 5);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CANTEEN_COOLDOWN.get(), 0) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(
                Math.round(
                        13F
                                - stack.getOrDefault(ModDataComponents.CANTEEN_COOLDOWN.get(), 0)
                                        * 13F
                                        / COOLDOWN_SECONDS),
                0,
                13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float remaining =
                (COOLDOWN_SECONDS - stack.getOrDefault(ModDataComponents.CANTEEN_COOLDOWN.get(), 0))
                        / (float) COOLDOWN_SECONDS;
        return Mth.hsvToRgb(Math.max(remaining, 0) / 3F, 1F, 1F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.canteen_vodka.cooldown"));
        adder.accept(Component.translatable("item.hbm.canteen_vodka.nausea"));
        adder.accept(Component.translatable("item.hbm.canteen_vodka.strength"));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable(
                        Polaroid.isBalefireDay()
                                ? "item.hbm.canteen_vodka.desc.P11"
                                : "item.hbm.canteen_vodka.desc"));
    }
}
