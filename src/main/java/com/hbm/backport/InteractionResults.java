// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Conversions between the 26.x InteractionResult (com.hbm.backport) and the three
 * types 1.21.1 uses in its place.
 *
 * Block interaction is where the mapping needs care. 26.x useItemOn returns
 * PASS to mean "skip the block, let the item be used" and TRY_WITH_EMPTY_HAND to
 * mean "fall through to useWithoutItem"; 1.21.1 spells those
 * SKIP_DEFAULT_BLOCK_INTERACTION and PASS_TO_DEFAULT_BLOCK_INTERACTION. The
 * defaults agree: vanilla's base useItemOn answers TRY_WITH_EMPTY_HAND in 26.x
 * and PASS_TO_DEFAULT_BLOCK_INTERACTION in 1.21.1.
 */
public final class InteractionResults {
    private InteractionResults() {}

    public static net.minecraft.world.InteractionResult toVanilla(InteractionResult result) {
        if (result instanceof InteractionResult.Success success) {
            if (!success.wasItemInteraction()) return net.minecraft.world.InteractionResult.SUCCESS_NO_ITEM_USED;
            return success.swingSource() == InteractionResult.SwingSource.NONE
                    ? net.minecraft.world.InteractionResult.CONSUME
                    : net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (result instanceof InteractionResult.Fail) return net.minecraft.world.InteractionResult.FAIL;
        return net.minecraft.world.InteractionResult.PASS;
    }

    public static InteractionResult fromVanilla(net.minecraft.world.InteractionResult result) {
        return switch (result) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case SUCCESS_NO_ITEM_USED -> InteractionResult.SUCCESS.withoutItem();
            case CONSUME, CONSUME_PARTIAL -> InteractionResult.CONSUME;
            case FAIL -> InteractionResult.FAIL;
            default -> InteractionResult.PASS;
        };
    }

    public static InteractionResultHolder<ItemStack> toHolder(InteractionResult result, ItemStack held) {
        ItemStack stack = held;
        if (result instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null) {
            stack = success.heldItemTransformedTo();
        }
        return new InteractionResultHolder<>(toVanilla(result), stack);
    }

    public static InteractionResult fromHolder(InteractionResultHolder<ItemStack> holder) {
        InteractionResult result = fromVanilla(holder.getResult());
        if (result instanceof InteractionResult.Success success) return success.heldItemTransformedTo(holder.getObject());
        return result;
    }

    public static ItemInteractionResult toItemInteraction(InteractionResult result) {
        if (result instanceof InteractionResult.Success success) {
            return success.swingSource() == InteractionResult.SwingSource.NONE
                    ? ItemInteractionResult.CONSUME
                    : ItemInteractionResult.SUCCESS;
        }
        if (result instanceof InteractionResult.Fail) return ItemInteractionResult.FAIL;
        if (result instanceof InteractionResult.TryEmptyHandInteraction) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    public static InteractionResult fromItemInteraction(ItemInteractionResult result) {
        return switch (result) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME, CONSUME_PARTIAL -> InteractionResult.CONSUME;
            case FAIL -> InteractionResult.FAIL;
            case PASS_TO_DEFAULT_BLOCK_INTERACTION -> InteractionResult.TRY_WITH_EMPTY_HAND;
            case SKIP_DEFAULT_BLOCK_INTERACTION -> InteractionResult.PASS;
        };
    }

    /**
     * 26.x hands inventoryTick the equipment slot the stack sits in (or null);
     * 1.21.1 hands a compartment index and a "selected" flag. The selected stack
     * is the main hand; otherwise the stack is looked up by identity in the
     * entity's equipment.
     */
    public static @Nullable EquipmentSlot slotOf(ItemStack stack, Entity entity, boolean selected) {
        if (selected) return EquipmentSlot.MAINHAND;
        if (entity instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (living.getItemBySlot(slot) == stack) return slot;
            }
        }
        return null;
    }
}
