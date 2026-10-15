// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.client.gui.ScreenWeaponTable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class ClientPlayerAccess {

    private ClientPlayerAccess() {}

    public static Component keyLabel(String name) {
        // backport: 1.21.1 has no public KeyMapping.get(name); createNameSupplier looks the mapping up
        // by name and returns its translated key message
        return KeyMapping.createNameSupplier(name).get();
    }

    public static @Nullable Player player() {
        return Minecraft.getInstance().player;
    }

    public static boolean firstPersonCamera() {
        return Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    public static boolean weaponTableOpen() {
        return Minecraft.getInstance().screen instanceof ScreenWeaponTable;
    }
}
