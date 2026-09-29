// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import net.minecraft.world.item.Item;
import com.hbm.backport.compat.ItemCompat;

public class ItemRTGPelletDepleted extends ItemCompat {

    private final DepletedRTGMaterial material;

    public ItemRTGPelletDepleted(Properties props, DepletedRTGMaterial material) {
        super(props);
        this.material = material;
    }

    public DepletedRTGMaterial getMaterial() {
        return material;
    }
}
