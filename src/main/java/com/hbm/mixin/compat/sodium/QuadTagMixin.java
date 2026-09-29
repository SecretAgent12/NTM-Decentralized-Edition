// backport: new file (no 26.x counterpart), see com.hbm.backport.client.sodium.SodiumDucks
package com.hbm.mixin.compat.sodium;

import com.hbm.backport.client.sodium.SodiumDucks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * backport: makes Sodium's QuadViewImpl a {@link SodiumDucks.QuadTag}. Its own {@code public
 * final int tag()} implements the interface method, so the mixin adds no code. 26.x used
 * QuadViewImpl#getTag() directly (Sodium on the compile classpath). Checked against Sodium
 * 0.8.13 for 1.21.1 (0.8.12 identical): net.caffeinemc.mods.sodium.client.render.frapi.mesh
 * .QuadViewImpl#tag()I.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.frapi.mesh.QuadViewImpl", remap = false)
public abstract class QuadTagMixin implements SodiumDucks.QuadTag {}
