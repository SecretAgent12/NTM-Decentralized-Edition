// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.block;

import net.minecraft.world.entity.Entity;

public interface IRadarCommandReceiver {

    boolean sendCommandPosition(int x, int y, int z);

    boolean sendCommandEntity(Entity target);

    /**
     * backport: a Sable sub-level (airship or other physics build) picked on the radar. It isn't an
     * entity, so it comes as its id plus where it is right now. By default: fire at that position.
     */
    default boolean sendCommandSubLevel(java.util.UUID subLevel, int x, int y, int z) {
        return sendCommandPosition(x, y, z);
    }
}
