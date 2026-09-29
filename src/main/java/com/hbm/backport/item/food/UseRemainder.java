// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 26.x {@code net.minecraft.world.item.component.UseRemainder} ({@code Item.Properties.usingConvertsTo}): what an
 * item turns into once used up (stew -> bowl). Stored as {@link ConsumableRegistry#USE_REMAINDER}; applied around
 * {@code Item.finishUsingItem} by {@code MixinBackportUseRemainder}, as 26.x ItemStack.finishUsingItem does.
 * backport: the remainder is item + count (26.x: an ItemStackTemplate, which may also carry components).
 */
public record UseRemainder(Item item, int count) {

    public static final Codec<UseRemainder> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("id")
                                                    .forGetter(UseRemainder::item),
                                            ExtraCodecs.POSITIVE_INT
                                                    .optionalFieldOf("count", 1)
                                                    .forGetter(UseRemainder::count))
                                    .apply(i, UseRemainder::new));

    public UseRemainder(Item item) {
        this(item, 1);
    }

    public ItemStack create() {
        return new ItemStack(item, count);
    }

    /** 26.x UseRemainder.convertIntoRemainder. */
    public ItemStack convertIntoRemainder(
            ItemStack stack, int originalCount, boolean infiniteMaterials, LivingEntity user) {
        if (infiniteMaterials) return stack;
        if (stack.getCount() >= originalCount) return stack;
        ItemStack remainder = create();
        if (stack.isEmpty()) return remainder;
        handleExtraItemsCreatedOnUse(user, remainder);
        return stack;
    }

    // backport: 26.x LivingEntity/Player.handleExtraItemsCreatedOnUse; unverified: non-players keep nothing
    private static void handleExtraItemsCreatedOnUse(LivingEntity user, ItemStack extra) {
        if (user.level().isClientSide()) return;
        if (user instanceof Player player && !player.getInventory().add(extra)) player.drop(extra, false);
    }

    /**
     * Applies the use remainder of {@code before} (a copy of the stack taken before it was used) to the stack the
     * use left behind; returns {@code after} unchanged when {@code before} has none.
     */
    public static ItemStack applyAfterUse(ItemStack before, ItemStack after, LivingEntity user) {
        UseRemainder remainder = before.get(ConsumableRegistry.USE_REMAINDER);
        if (remainder == null) return after;
        return remainder.convertIntoRemainder(after, before.getCount(), user.hasInfiniteMaterials(), user);
    }
}
