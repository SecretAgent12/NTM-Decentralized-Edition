// backport: new file (no 26.x counterpart), see com.hbm.backport.client.sodium.SodiumDucks
package com.hbm.mixin.compat.sodium;

import com.hbm.backport.client.sodium.SodiumDucks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/**
 * backport: exposes the lightmap array of Sodium's QuadLightData ({@code public final int[] lm})
 * as {@link SodiumDucks.QuadLightData}; 26.x wrote {@code out.lm} directly. Checked against
 * Sodium 0.8.13 for 1.21.1 (0.8.12 identical).
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData", remap = false)
public abstract class QuadLightDataMixin implements SodiumDucks.QuadLightData {
    @Shadow @Final public int[] lm;

    @Override
    public int[] hbm$lightmaps() {
        return lm;
    }
}
