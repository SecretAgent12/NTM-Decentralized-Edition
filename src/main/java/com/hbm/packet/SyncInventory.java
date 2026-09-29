// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import java.util.Arrays;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;

public final class SyncInventory extends NonNullList<ItemStack> implements SyncSource {
    private record Layout(
            boolean all, boolean components, long low, long[] high, int maximum, long units) {}

    private static final ClassValue<Layout> SLOTS =
            new ClassValue<>() {
                @Override
                protected Layout computeValue(Class<?> type) {
                    for (Class<?> current = type;
                            current != null;
                            current = current.getSuperclass()) {
                        SyncSlots slots = current.getDeclaredAnnotation(SyncSlots.class);
                        if (slots == null) continue;
                        int[] selected = slots.value();
                        int maximum = -1;
                        for (int slot : selected) {
                            if (slot < 0) throw new IllegalArgumentException("Sync slot " + slot);
                            maximum = Math.max(maximum, slot);
                        }
                        long low = 0;
                        long[] high = maximum < 64 ? null : new long[maximum >>> 6];
                        for (int slot : selected) {
                            if (slot < 64) low |= 1L << slot;
                            else high[(slot >>> 6) - 1] |= 1L << slot;
                        }
                        return new Layout(
                                slots.all(), slots.components(), low, high, maximum, slots.units());
                    }
                    return null;
                }
            };

    private final boolean all;
    private final boolean components;
    private final long mask;
    private final long[] high;
    private final long units;

    private SyncInventory(ItemStack[] items, Layout slots) {
        super(Arrays.asList(items), ItemStack.EMPTY);
        all = slots.all();
        components = slots.components();
        mask = slots.low();
        high = slots.high();
        units = slots.units();
    }

    public static NonNullList<ItemStack> create(Object owner, int size) {
        Layout slots = SLOTS.get(owner.getClass());
        if (slots == null) return NonNullList.withSize(size, ItemStack.EMPTY);
        if (slots.maximum() >= size)
            throw new IllegalArgumentException("Sync slot " + slots.maximum());
        ItemStack[] items = new ItemStack[size];
        Arrays.fill(items, ItemStack.EMPTY);
        return new SyncInventory(items, slots);
    }

    private boolean watches(int slot) {
        return all
                || (slot < 64
                        ? (mask & (1L << slot)) != 0
                        : high != null
                                && (slot >>> 6) <= high.length
                                && (high[(slot >>> 6) - 1] & (1L << slot)) != 0);
    }

    boolean components() {
        return components;
    }

    long units() {
        return units;
    }

    @Override
    public ItemStack set(int index, ItemStack item) {
        ItemStack previous = super.set(index, item);
        if (previous != item && watches(index)) {
            if (syncBound()) {
                SyncBindings.unbind(this, previous);
                SyncBindings.bind(this, item, 3);
            }
            if (components
                    || previous.getItem() != item.getItem()
                    || previous.getCount() != item.getCount()) syncChanged(3);
        }
        return previous;
    }

    void bindItems() {
        for (int i = 0; i < size(); i++) if (watches(i)) SyncBindings.bind(this, get(i), 3);
    }

    void unbindItems() {
        for (int i = 0; i < size(); i++) if (watches(i)) SyncBindings.unbind(this, get(i));
    }


    // backport: woven trait SyncSource
    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
