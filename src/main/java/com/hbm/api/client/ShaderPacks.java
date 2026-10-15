// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.client;

import com.hbm.backport.client.compat.iris.IrisCompat;

public final class ShaderPacks {
    private ShaderPacks() {}

    // backport: Iris is not a compile dependency of DE; its public API is reached reflectively
    // through IrisCompat (false without Iris)
    public static boolean inUse() {
        return IrisCompat.isShaderPackInUse();
    }
}
