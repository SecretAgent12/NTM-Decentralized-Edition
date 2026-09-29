// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import net.minecraft.world.item.Item;
import com.hbm.backport.compat.ItemCompat;

public class ItemGrenade extends ItemCompat {

    public final int fuse;

    public ItemGrenade(Properties properties, int fuse) {
        super(properties);
        this.fuse = fuse;
    }
}
