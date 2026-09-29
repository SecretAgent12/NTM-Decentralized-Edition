// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.entity.grenade.EntityDisperserCanister;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.ItemFluidTank;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class ItemDisperser extends ItemFluidTank {

    private final Kind kind;
    private final String base;
    private final ResourceLocation icon;
    private final ResourceLocation overlayIcon;

    public ItemDisperser(
            Item.Properties properties,
            int capacity,
            String base,
            Kind kind,
            ResourceLocation icon,
            ResourceLocation overlayIcon) {
        super(properties, capacity, base, Family.DISPERSER);
        this.kind = kind;
        this.base = base;
        this.icon = icon;
        this.overlayIcon = overlayIcon;
    }

    @Override
    protected @Nullable ItemStackTemplate remainder(ItemStack consumed) {
        return getCraftingRemainder();
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS,
                    0.5F,
                    0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            EntityDisperserCanister canister = new EntityDisperserCanister(level, player);
            canister.setDisperserItem(this);
            canister.setFluid(getContent(stack).type());
            level.addFreshEntity(canister);
            stack.consume(1, player);
        }

        return InteractionResult.SUCCESS;
    }

    public boolean isCreativeVariant(Fluid fluid) {
        return switch (kind) {
            case CANISTER -> canStore(fluid);
            case GLAND -> fluid == NTMFluids.PHEROMONE || fluid == NTMFluids.SULFURIC_ACID;
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        Fluid fluid = getContent(stack).type();
        Component other =
                fluid == Fluids.EMPTY
                        ? Component.translatable("hbmfluid.none")
                        : NTMFluidProperties.getDisplayName(fluid);
        Component self = Component.translatable("item.hbm." + base);
        return kind == Kind.GLAND
                ? Component.empty().append(other).append(" ").append(self)
                : Component.empty().append(self).append(" ").append(other);
    }

    public ResourceLocation iconTexture(int pass) {
        return pass == 1 ? overlayIcon : icon;
    }

    public enum Kind {
        CANISTER,
        GLAND
    }
}
