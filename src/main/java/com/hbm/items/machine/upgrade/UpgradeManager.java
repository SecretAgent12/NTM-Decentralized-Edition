// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine.upgrade;

import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.tileentity.IUpgradeInfoProvider;
import java.util.Arrays;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import com.hbm.packet.SyncBindings;

public final class UpgradeManager implements SyncSource {

    private final IUpgradeInfoProvider provider;
    @SyncField private final int[] levels = new int[UpgradeType.VALUES.length];
    private final int[] nextLevels = new int[UpgradeType.VALUES.length];

    public UpgradeManager(IUpgradeInfoProvider provider) {
        this.provider = provider;
    }

    public void scan(Container container, int start, int end) {
        int[] caps = provider.getValidUpgrades();
        Arrays.fill(nextLevels, 0);

        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!(stack.getItem() instanceof ItemMachineUpgrade upgrade)) continue;
            int ord = upgrade.type.ordinal();
            int cap = caps[ord];
            if (cap <= 0) continue;
            nextLevels[ord] = Math.min(nextLevels[ord] + upgrade.tier, cap);
        }
        for (int i = 0; i < levels.length; i++) levels[i] = nextLevels[i];
    }

    public int getLevel(UpgradeType type) {
        return levels[type.ordinal()];
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
