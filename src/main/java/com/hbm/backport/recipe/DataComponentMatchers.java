// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.world.item.ItemStack;

/**
 * 26.x DataComponentMatchers: exact component values plus partial predicates, as
 * the "components" and "predicates" fields of an item predicate. In 1.21.1 the same
 * two fields live in ItemPredicate: the exact part is DataComponentPredicate, the
 * partial ones are ItemSubPredicates (registry minecraft:item_sub_predicate_type),
 * so JSON written for 26.x reads the same way.
 *
 * Naming trap: 26.x's DataComponentPredicate is the PARTIAL interface; 1.21.1's
 * DataComponentPredicate is the EXACT class (26.x DataComponentExactPredicate).
 */
public record DataComponentMatchers(
        DataComponentPredicate exact, Map<ItemSubPredicate.Type<?>, ItemSubPredicate> partial)
        implements Predicate<ItemStack> {

    public static final DataComponentMatchers ANY = new DataComponentMatchers(DataComponentPredicate.EMPTY, Map.of());

    public static final MapCodec<DataComponentMatchers> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DataComponentPredicate.CODEC.optionalFieldOf("components", DataComponentPredicate.EMPTY)
                    .forGetter(DataComponentMatchers::exact),
            ItemSubPredicate.CODEC.optionalFieldOf("predicates", Map.of())
                    .forGetter(DataComponentMatchers::partial))
            .apply(i, DataComponentMatchers::new));

    public boolean isEmpty() {
        return exact.alwaysMatches() && partial.isEmpty();
    }

    @Override
    public boolean test(ItemStack stack) {
        if (!exact.test(stack)) return false;
        for (ItemSubPredicate p : partial.values()) if (!p.matches(stack)) return false;
        return true;
    }
}
