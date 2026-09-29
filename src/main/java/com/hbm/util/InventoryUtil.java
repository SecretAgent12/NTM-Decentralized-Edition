// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.blocks.network.CraneInserter;
import com.hbm.capability.NtmContracts;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe.AnvilOutput;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.tileentity.machine.BlockEntityFurnaceBrick;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class InventoryUtil {

    private InventoryUtil() {}

    public static @Nullable Container containerAt(Level level, BlockPos pos) {
        Container declared = NtmContracts.INVENTORY.at(level, pos);
        if (declared != null) return declared;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WorldlyContainerHolder holder)
            return holder.getContainer(state, level, pos);
        if (!state.hasBlockEntity() || !(level.getBlockEntity(pos) instanceof Container container))
            return null;
        return container instanceof ChestBlockEntity && state.getBlock() instanceof ChestBlock chest
                ? ChestBlock.getContainer(chest, state, level, pos, true)
                : container;
    }

    public static boolean inventoryAt(Level level, BlockPos pos, @Nullable Direction face) {
        return ForeignItems.present(level, pos, face);
    }

    public static ItemStack insertAt(
            Level level, BlockPos pos, @Nullable Direction face, ItemStack toAdd) {
        if (toAdd.isEmpty()) return toAdd;
        toAdd.setCount(ForeignItems.insert(level, pos, face, toAdd.copy()).getCount());
        return toAdd;
    }

    // backport: 26.x does this with the generic StackedContents<ItemStack> (account/tryPick);
    // 1.21.1's StackedContents only counts plain item ids, so the same pick is done here:
    // each unit of each ingredient is matched to a distinct item in the inventory
    // (bipartite matching with augmenting paths, like tryPick with a multiplier of 1).
    private static boolean takeIngredients(
            Player player, List<CountIngredient> ingredients, boolean commit) {
        Inventory inventory = player.getInventory();
        List<ItemStack> distinct = new ArrayList<>();
        List<Integer> available = new ArrayList<>();

        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            ItemStack key = intern(distinct, stack, commit);
            int index = indexOf(distinct, key);
            if (index == available.size()) available.add(0);
            available.set(index, available.get(index) + stack.getCount());
        }

        List<CountIngredient> demand = new ArrayList<>();
        for (CountIngredient ingredient : ingredients) {
            for (int i = 0; i < ingredient.count(); i++) demand.add(ingredient);
        }

        int[] assigned = new int[demand.size()];
        java.util.Arrays.fill(assigned, -1);
        int[] used = new int[distinct.size()];
        for (int unit = 0; unit < demand.size(); unit++) {
            if (!pick(unit, demand, distinct, available, assigned, used, new boolean[distinct.size()]))
                return false;
        }

        if (commit) {
            List<ItemStack> spent = new ArrayList<>();
            for (int item : assigned) spent.add(distinct.get(item));
            spend(inventory, spent);
        }
        return true;
    }

    private static boolean pick(
            int unit,
            List<CountIngredient> demand,
            List<ItemStack> distinct,
            List<Integer> available,
            int[] assigned,
            int[] used,
            boolean[] visited) {
        for (int item = 0; item < distinct.size(); item++) {
            if (visited[item] || !demand.get(unit).acceptsItem(distinct.get(item))) continue;
            visited[item] = true;
            if (used[item] < available.get(item)) {
                assigned[unit] = item;
                used[item]++;
                return true;
            }
            for (int other = 0; other < assigned.length; other++) {
                if (assigned[other] != item) continue;
                assigned[other] = -1;
                used[item]--;
                if (pick(other, demand, distinct, available, assigned, used, visited)) {
                    assigned[unit] = item;
                    used[item]++;
                    return true;
                }
                assigned[other] = item;
                used[item]++;
            }
        }
        return false;
    }

    private static int indexOf(List<ItemStack> distinct, ItemStack key) {
        for (int i = 0; i < distinct.size(); i++) if (distinct.get(i) == key) return i;
        return -1;
    }

    private static ItemStack intern(List<ItemStack> distinct, ItemStack stack, boolean commit) {
        for (ItemStack known : distinct) {
            if (ItemStack.isSameItemSameComponents(known, stack)) return known;
        }
        ItemStack key = commit ? stack.copyWithCount(1) : stack;
        distinct.add(key);
        return key;
    }

    private static void spend(Inventory inventory, List<ItemStack> spent) {
        for (ItemStack unit : spent) {
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, unit)) continue;
                stack.shrink(1);
                if (stack.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
                break;
            }
        }
    }

    public static boolean hasIngredients(Player player, List<CountIngredient> ingredients) {
        return takeIngredients(player, ingredients, false);
    }

    public static boolean consumeIngredients(Player player, List<CountIngredient> ingredients) {
        return takeIngredients(player, ingredients, true);
    }

    public static ItemStack exchangeHeld(Player player, ItemStack held, ItemStack remainder) {
        held.shrink(1);
        if (held.isEmpty()) return remainder;
        player.getInventory().placeItemBackInInventory(remainder);
        return held;
    }

    public static void giveChanceOutputs(Player player, List<AnvilOutput> outputs) {
        RandomSource random = player.getRandom();
        for (AnvilOutput out : outputs) {
            if (out.chance() == 1.0F || random.nextFloat() < out.chance()) {

                player.getInventory().placeItemBackInInventory(out.stack().copy());
            }
        }
    }

    private interface Slots {

        ItemStack get(int slot);

        void set(int slot, ItemStack stack);
    }

    private static Slots slotsOf(List<ItemStack> inv) {
        return new Slots() {
            @Override
            public ItemStack get(int slot) {
                return inv.get(slot);
            }

            @Override
            public void set(int slot, ItemStack stack) {
                inv.set(slot, stack);
            }
        };
    }

    private static Slots slotsOf(Container inv) {
        return new Slots() {
            @Override
            public ItemStack get(int slot) {
                return inv.getItem(slot);
            }

            @Override
            public void set(int slot, ItemStack stack) {
                inv.setItem(slot, stack);
            }
        };
    }

    private static ItemStack topUp(Slots inv, int start, int end, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        for (int i = start; i <= end; i++) {
            ItemStack slot = inv.get(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, stack)) continue;

            int transfer = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());

            if (transfer > 0) {
                slot.grow(transfer);
                stack.shrink(transfer);

                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private static boolean fillFreeSlot(Slots inv, int start, int end, ItemStack stack) {
        if (stack.isEmpty()) return true;

        for (int i = start; i <= end; i++) {
            if (inv.get(i).isEmpty()) {

                inv.set(i, stack.copy());
                stack.setCount(0);
                return true;
            }
        }

        return false;
    }

    private static ItemStack insert(Slots inv, int start, int end, ItemStack stack) {
        ItemStack rem = topUp(inv, start, end, stack);

        if (rem.isEmpty()) return ItemStack.EMPTY;

        return fillFreeSlot(inv, start, end, rem) ? ItemStack.EMPTY : rem;
    }

    public static ItemStack tryAddItemToExistingStack(
            List<ItemStack> inv, int start, int end, ItemStack stack) {
        return topUp(slotsOf(inv), start, end, stack);
    }

    public static boolean tryAddItemToNewSlot(
            List<ItemStack> inv, int start, int end, ItemStack stack) {
        return fillFreeSlot(slotsOf(inv), start, end, stack);
    }

    public static ItemStack tryAddItemToInventory(
            List<ItemStack> inv, int start, int end, ItemStack stack) {
        return insert(slotsOf(inv), start, end, stack);
    }

    public static ItemStack tryAddItemToExistingStack(
            Container inv, int start, int end, ItemStack stack) {
        return topUp(slotsOf(inv), start, end, stack);
    }

    public static boolean tryAddItemToNewSlot(Container inv, int start, int end, ItemStack stack) {
        return fillFreeSlot(slotsOf(inv), start, end, stack);
    }

    public static ItemStack tryAddItemToInventory(
            Container inv, int start, int end, ItemStack stack) {
        return insert(slotsOf(inv), start, end, stack);
    }

    public static boolean doesArrayHaveSpace(
            List<ItemStack> inv, int start, int end, ItemStack[] items) {
        List<ItemStack> copy = new ArrayList<>(end - start + 1);
        for (int i = start; i <= end; i++) copy.add(inv.get(i).copy());

        Slots slots = slotsOf(copy);
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            if (!insert(slots, 0, copy.size() - 1, item.copy()).isEmpty()) return false;
        }

        return true;
    }
}
