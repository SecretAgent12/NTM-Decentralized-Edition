// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.platform.Services;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public final class BlockReadBounds {
    private static final boolean CLIENT = Services.PLATFORM.isPhysicalClient();

    private BlockReadBounds() {}

    public static boolean canRead(BlockGetter view, BlockPos pos) {
        return canRead(view, pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean canRead(BlockGetter view, int x, int y, int z) {
        return !CLIENT || Client.canRead(view, x, y, z);
    }

    /**
     * backport: 26.x RenderSectionRegion is a 3x3x3 block of sections (min section x/y/z public
     * via AT); 1.21.1 RenderChunkRegion is 3x3 whole chunk columns (RADIUS 1, SIZE 3) with the
     * lower corner chunk in private minChunkX/minChunkZ, and reads above/below the build height
     * return air instead of failing, so only x/z are bounded. The corner is read by reflection
     * (runtime uses Mojang names); if that fails, reads are allowed as before.
     */
    private static final class Client {
        private static final java.lang.invoke.VarHandle MIN_X = field("minChunkX");
        private static final java.lang.invoke.VarHandle MIN_Z = field("minChunkZ");

        private static java.lang.invoke.VarHandle field(String name) {
            try {
                java.lang.reflect.Field f = RenderChunkRegion.class.getDeclaredField(name);
                f.setAccessible(true);
                return java.lang.invoke.MethodHandles.lookup().unreflectVarHandle(f);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }

        private static boolean canRead(BlockGetter view, int blockX, int blockY, int blockZ) {
            if (!(view instanceof RenderChunkRegion region) || MIN_X == null || MIN_Z == null)
                return true;
            int x = (blockX >> 4) - (int) MIN_X.get(region);
            int z = (blockZ >> 4) - (int) MIN_Z.get(region);
            return x >= 0 && x < RenderChunkRegion.SIZE && z >= 0 && z < RenderChunkRegion.SIZE;
        }
    }
}
