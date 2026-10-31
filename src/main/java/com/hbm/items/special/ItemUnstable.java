// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.compat.ItemCompat;

/**
 * U-238m2: decays in 10 seconds and then goes off like a nuke.
 *
 * <p>backport-fix: BF-120 the timer runs only while the ingot is in a player's inventory, as in
 * 1.7.10 (there {@code Item.onUpdate}, called only for the player's inventory, counted it up in the
 * stack's NBT). Anywhere else - a machine, a chest, the ground - it stands still. NEXT turned it
 * into a deadline on the world clock, watched in every inventory (UnstableFuses), so the ingot also
 * went off inside the plasma forge and its FEnSU recipe (6000 ticks) could never finish. The ticks
 * spent are kept in {@code UNSTABLE_TIMER}; a stack still carrying NEXT's deadline ({@code
 * UNSTABLE_AT}) is converted the first time it ticks in a player's inventory, and that deadline no
 * longer sets anything off by itself.
 */
public final class ItemUnstable extends ItemCompat implements IItemEntityUpdate {

    public static final int RADIUS = 350;
    public static final int TIMER = 200;

    public ItemUnstable(Properties properties) {
        super(properties);
    }

    private static int getTimer(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.UNSTABLE_TIMER.get(), 0);
    }

    /** NEXT's world-clock fuse is no longer watched: an old stack goes off only in a player's hands. */
    public static boolean hasSpentFuse(ItemStack stack, long gameTime) {
        return false;
    }

    public static void detonate(ServerLevel level, double x, double y, double z) {
        level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, RADIUS, x, y, z));
        level.playSound(
                null,
                BlockPos.containing(x, y, z),
                ModSounds.OLD_EXPLOSION.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (stack.isEmpty() || !(entity instanceof Player)) return;
        int timer = getTimer(stack);
        Long ends = stack.remove(ModDataComponents.UNSTABLE_AT.get());
        if (ends != null)
            timer = (int) Math.max(0L, Math.min(TIMER - 1, TIMER - (ends - level.getGameTime())));
        timer++;
        if (timer >= TIMER) {
            detonate(level, entity.getX(), entity.getY(), entity.getZ());
            stack.setCount(0);
            return;
        }
        stack.set(ModDataComponents.UNSTABLE_TIMER.get(), timer);
    }

    /** On the ground the timer stands still, as in 1.7.10 (an item entity never ticked its item). */
    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "desc.item.unstable.decay", (getTimer(stack) * 100 / TIMER) + "%"));
    }
}
