// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.food;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.UseAnim;

/** 26.x {@code net.minecraft.world.item.component.Consumables}: the defaults the mod uses. */
public final class Consumables {

    public static final float DEFAULT_CONSUME_SECONDS = Consumable.DEFAULT_CONSUME_SECONDS;
    public static final Consumable DEFAULT_FOOD = defaultFood().build();
    public static final Consumable DEFAULT_DRINK = defaultDrink().build();

    private Consumables() {}

    public static Consumable.Builder defaultFood() {
        return Consumable.builder()
                .consumeSeconds(DEFAULT_CONSUME_SECONDS)
                .animation(UseAnim.EAT)
                .sound(SoundEvents.GENERIC_EAT)
                .hasConsumeParticles(true);
    }

    public static Consumable.Builder defaultDrink() {
        return Consumable.builder()
                .consumeSeconds(DEFAULT_CONSUME_SECONDS)
                .animation(UseAnim.DRINK)
                .sound(SoundEvents.GENERIC_DRINK)
                .hasConsumeParticles(false);
    }
}
