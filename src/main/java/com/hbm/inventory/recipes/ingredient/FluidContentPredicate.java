// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

// backport: 26.x partial component predicates (DataComponentPredicate) are 1.21.1
// ItemSubPredicates, registered as minecraft:item_sub_predicate_type.
public record FluidContentPredicate(Fluid fluid) implements ItemSubPredicate {

    public static final Codec<FluidContentPredicate> CODEC =
            BuiltInRegistries.FLUID
                    .byNameCodec()
                    .xmap(FluidContentPredicate::new, FluidContentPredicate::fluid);

    public static RegistryHandle<ItemSubPredicate.Type<FluidContentPredicate>> TYPE;

    public static void register(IRegistrar r) {
        TYPE = r.registerDataComponentPredicate("fluid_content", CODEC);
    }

    @Override
    public boolean matches(ItemStack components) {
        FluidStackNTM content = components.get(ModDataComponents.FLUID_CONTENT.get());
        return (content == null ? Fluids.EMPTY : content.type()) == fluid;
    }
}
