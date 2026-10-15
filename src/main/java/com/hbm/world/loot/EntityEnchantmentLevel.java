// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public record EntityEnchantmentLevel(
        LootContext.EntityTarget entity, Holder<Enchantment> enchantment)
        implements NumberProvider {

    public static final MapCodec<EntityEnchantmentLevel> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            LootContext.EntityTarget.CODEC
                                                    .fieldOf("entity")
                                                    .forGetter(EntityEnchantmentLevel::entity),
                                            Enchantment.CODEC
                                                    .fieldOf("enchantment")
                                                    .forGetter(EntityEnchantmentLevel::enchantment))
                                    .apply(i, EntityEnchantmentLevel::new));

    // backport: 1.21.1 number providers name a LootNumberProviderType instead of their codec
    @Override
    public net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType getType() {
        return com.hbm.backport.LootTypes.numberProvider(MAP_CODEC);
    }

    @Override
    public float getFloat(LootContext context) {
        return context.getParamOrNull(entity.getParam()) instanceof LivingEntity living
                ? EnchantmentHelper.getEnchantmentLevel(enchantment, living)
                : 0F;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(entity.getParam());
    }
}
