// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import com.hbm.backport.ColorFamilies;

public final class MaterialDyeItem extends MaterialShapeItem implements SignApplicator {

    // backport: 26.x dye items carry their colour in the DYE data component; 1.21.1 has no
    // such component, so the colour is a field and the dye behaviour is the vanilla dye's
    private final DyeColor dye;

    public MaterialDyeItem(
            Properties properties, NTMMaterial material, MaterialShapes shape, DyeColor dye) {
        super(properties, material, shape);
        this.dye = dye;
    }

    public DyeColor dye() {
        return dye;
    }

    @Override
    public InteractionResult backport$interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return com.hbm.backport.InteractionResults.fromVanilla(
                net.minecraft.world.item.DyeItem.byColor(dye)
                        .interactLivingEntity(stack, player, target, hand));
    }

    @Override
    public boolean tryApplyToSign(Level level, SignBlockEntity sign, boolean front, Player player) {
        return net.minecraft.world.item.DyeItem.byColor(dye).tryApplyToSign(level, sign, front, player);
    }
}
