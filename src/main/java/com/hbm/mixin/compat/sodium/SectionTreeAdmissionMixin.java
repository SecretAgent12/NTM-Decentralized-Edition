// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.client.model.SectionGeometry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A section that holds only air but carries section geometry is admitted to Sodium's renderable
 * section tree and queued for its initial build, instead of being recorded as built-empty (which
 * never schedules a meshing task, so its AddSectionGeometryEvent renderers would never run).
 *
 * backport: target checked against Sodium 0.8.13 for 1.21.1 (0.8.12 identical):
 * RenderSectionManager.onSectionAdded(int x, int y, int z) reads
 * {@code chunk.getSections()[...].hasOnlyAir()} once; field {@code private final ClientLevel
 * level}. The section coordinates come from @Local(argsOnly) ordinals instead of trailing handler
 * parameters (same values as 26.x).
 */
@Pseudo
@Mixin(
        targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager",
        remap = false)
public class SectionTreeAdmissionMixin {
    @Shadow @Final private ClientLevel level;

    @ModifyExpressionValue(
            method = "onSectionAdded",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z"))
    private boolean hbm$includeGeometry(
            boolean empty,
            @Local(argsOnly = true, ordinal = 0) int x,
            @Local(argsOnly = true, ordinal = 1) int y,
            @Local(argsOnly = true, ordinal = 2) int z) {
        return empty && !SectionGeometry.has(level, SectionPos.asLong(x, y, z));
    }
}
