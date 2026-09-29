// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.hazard;

import com.hbm.hazard.HazardSystem;
import com.tiviacz.travelersbackpack.TravelersBackpack;
import com.tiviacz.travelersbackpack.capability.AttachmentUtils;
import net.minecraft.world.entity.player.Player;

/**
 * The backpack worn in Traveler's Backpack's own back slot counts as carried equipment.
 *
 * <p>backport: ntm-next's com.hbm.hazard.TravelersEquipmentHazards; on 1.21.1 AttachmentUtils
 * lives in the capability package. With the Curios/Accessories integration enabled the backpack
 * sits in a Curios slot instead, which CuriosHazards already covers (hence 0 here, as in next).
 */
public final class TravelersEquipmentHazards {
    private TravelersEquipmentHazards() {}

    public static float apply(Player player) {
        return TravelersBackpack.enableIntegration()
                ? 0F
                : HazardSystem.applyExternalEquipment(AttachmentUtils.getWearingBackpack(player), player);
    }
}
