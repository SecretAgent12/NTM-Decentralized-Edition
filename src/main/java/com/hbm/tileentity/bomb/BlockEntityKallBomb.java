// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuKallBomb;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * BlockEntity для Kall Bomb.
 * Слоты полностью идентичны Толстяку (Fat Man):
 *   0: взрывчатка (Igniter)
 *   1-4: 4 взрывные линзы (Early Explosive Lenses / Explosive Lenses)
 *   5: вместо плутониевого ядра — слот для контейнеров с токсичными отходами (watz_mud, wastefluid)
 */
public class BlockEntityKallBomb extends BlockEntityMachineBase implements IGUIProvider {

    public static final int SLOT_IGNITER = 0;
    public static final int SLOT_LENS_1 = 1;
    public static final int SLOT_LENS_2 = 2;
    public static final int SLOT_LENS_3 = 3;
    public static final int SLOT_LENS_4 = 4;
    public static final int SLOT_WASTE = 5;
    public static final int SLOT_COUNT = 6;

    public BlockEntityKallBomb(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KALL_BOMB.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    public static boolean isLens(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ModItems.EARLY_EXPLOSIVE_LENSES.get())
                || stack.is(ModItems.EXPLOSIVE_LENSES.get());
    }

    public static boolean isIgniter(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ModItems.MAN_IGNITER.get())
                || stack.is(ModItems.IGNITER.get());
    }

    public static boolean isWasteFluid(@Nullable Fluid fluid) {
        if (fluid == null) return false;
        return fluid == NTMFluids.WATZ_MUD
                || fluid == NTMFluids.WASTEFLUID
                || fluid == NTMFluids.REDMUD
                || fluid == ModBlocks.TOXIC_FLUID.source().get();
    }

    public static @Nullable FluidStackNTM getFluidContent(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof IFluidContainerItem container) {
            return container.getContent(stack);
        }
        FluidStackNTM ntmContent = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (ntmContent != null) return ntmContent;
        if (stack.is(ModItems.BUCKET_TOXIC.get())) {
            return new FluidStackNTM(ModBlocks.TOXIC_FLUID.source().get(), 1000L);
        }
        return null;
    }

    /**
     * Подсчитывает суммарное количество миллибакетов токсичных отходов (WATZ_MUD / WASTEFLUID) в стеке.
     * Корректно учитывает все типы контейнеров мода (баки, канистры, вёдра).
     */
    public static long getToxicWasteMb(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        FluidStackNTM content = getFluidContent(stack);
        if (content != null && isWasteFluid(content.type()) && content.amount() > 0) {
            return content.amount() * (long) stack.getCount();
        }
        return 0;
    }

    /**
     * Проверяет, является ли содержимое слота токсичных отходов валидным контейнером с токсичными отходами.
     */
    public static boolean isValidWasteContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FluidStackNTM content = getFluidContent(stack);
        return content != null && isWasteFluid(content.type()) && content.amount() > 0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_IGNITER -> isIgniter(stack);
            case SLOT_LENS_1, SLOT_LENS_2, SLOT_LENS_3, SLOT_LENS_4 -> isLens(stack);
            case SLOT_WASTE -> isValidWasteContainer(stack) || stack.getItem() instanceof IFluidContainerItem;
            default -> false;
        };
    }

    /**
     * Проверяет полный состав бомбы:
     * - детонатор (MAN_IGNITER / IGNITER)
     * - 4 линзы (EARLY_EXPLOSIVE_LENSES / EXPLOSIVE_LENSES)
     * - контейнер с токсичными отходами (watz_mud / wastefluid)
     */
    public static boolean isReady(Container c) {
        return isLens(c.getItem(SLOT_LENS_1))
                && isLens(c.getItem(SLOT_LENS_2))
                && isLens(c.getItem(SLOT_LENS_3))
                && isLens(c.getItem(SLOT_LENS_4))
                && isIgniter(c.getItem(SLOT_IGNITER))
                && isValidWasteContainer(c.getItem(SLOT_WASTE));
    }

    public boolean isReady() {
        return isReady(this);
    }

    public long getWasteMb() {
        return getToxicWasteMb(getItem(SLOT_WASTE));
    }

    public void clearSlots() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            setItem(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.kallBomb");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuKallBomb(containerId, playerInventory, this);
    }
}
