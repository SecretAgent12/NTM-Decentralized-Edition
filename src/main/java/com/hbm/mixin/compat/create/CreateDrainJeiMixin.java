// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.create;

import com.hbm.items.machine.IFluidContainerItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * backport-fix: BF-069 Create's JEI page for the Item Drain showed NTM canisters, gas tanks and
 * other fluid containers coming out full ("gas tank with X -> X + the same full gas tank"), while
 * the drain itself empties them correctly.
 *
 * <p>Create builds that page by draining a copy of each JEI item and taking the emptied container,
 * but when the emptied container is the same item as the input it shows the input stack instead
 * ({@code ItemHelper.sameItem} only compares the item). NTM containers stay the same item when
 * empty (the content is a data component), so the full input was shown as the result. For NTM
 * fluid containers the check here also compares the components; other items are left as Create
 * does it.
 *
 * <p>Create is optional: the target is named by string, the mixin is applied only when Create is
 * installed (ClientCompatPlugin) and {@code require = 0} skips it if Create changes the method.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.compat.jei.category.ItemDrainCategory", remap = false)
public abstract class CreateDrainJeiMixin {

    @WrapOperation(
            method = "consumeRecipes",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/simibubi/create/foundation/item/ItemHelper;sameItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 0)
    private static boolean hbm$sameItemAndContent(
            ItemStack input, ItemStack result, Operation<Boolean> original) {
        if (input.getItem() instanceof IFluidContainerItem)
            return ItemStack.isSameItemSameComponents(input, result);
        return original.call(input, result);
    }
}
