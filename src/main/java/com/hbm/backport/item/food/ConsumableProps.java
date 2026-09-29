// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/**
 * 26.x {@code Item.Properties} methods 1.21.1 lacks; the backport rewrites {@code props.food(food, consumable)} and
 * {@code props.usingConvertsTo(item)} to these.
 *
 * <p>26.x {@code food(food)} is {@code food(food, Consumables.DEFAULT_FOOD)}; on 1.21.1 the one-argument form stays
 * native (1.21.1 eating = DEFAULT_FOOD: 1.6 s, eat animation, eat sound and particles, burp), so only the
 * two-argument form adds the backport consumable component.
 */
public final class ConsumableProps {

    private ConsumableProps() {}

    public static Item.Properties food(Item.Properties props, FoodProperties food, Consumable consumable) {
        return props.food(food).component(ConsumableRegistry.CONSUMABLE, consumable);
    }

    public static Item.Properties usingConvertsTo(Item.Properties props, ItemLike item) {
        return props.component(ConsumableRegistry.USE_REMAINDER, new UseRemainder(item.asItem()));
    }
}
