// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

// backport: new file (no 26.x counterpart), see com.hbm.backport.client.sodium.SodiumDucks
package com.hbm.mixin.compat.sodium;

import com.hbm.backport.client.sodium.SodiumDucks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * backport: makes Sodium's LightDataAccess a {@link SodiumDucks.LightCache}. Its own {@code
 * public BlockAndTintGetter getLevel()} implements the interface method, so the mixin adds no
 * code. Checked against Sodium 0.8.13 for 1.21.1 (0.8.12 identical):
 * net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess#getLevel()
 * Lnet/minecraft/world/level/BlockAndTintGetter;.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess", remap = false)
public abstract class LightCacheMixin implements SodiumDucks.LightCache {}
