// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.sodium;

import com.hbm.client.model.SectionGeometry;
import com.hbm.interfaces.injected.IClientCoreHint;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sodium's LevelSlice is the level view its chunk meshing hands to block models and to
 * AddSectionGeometryEvent renderers (the 1.21.1 counterpart of vanilla's RenderChunkRegion, see
 * MixinTerrainModelRegion):
 *
 * <ul>
 *   <li>TerrainView: SectionedModel draws nothing through the block model path there, because
 *       the section geometry index draws the machine (split per section) instead. Without it the
 *       whole machine model is meshed a second time at the core block, coplanar with the section
 *       geometry (z-fighting, and translucent parts flip order with Sodium's translucency sort);
 *   <li>IClientCoreHint: the multiblock core lookup cache of the view;
 *   <li>prepare: an air-only section still gets a meshing task (and so its
 *       AddSectionGeometryEvent renderers) when section geometry reaches into it.
 * </ul>
 *
 * backport: target checked against Sodium 0.8.13 for 1.21.1 (0.8.12 identical): static
 * LevelSlice.prepare(Level, SectionPos, ClonedChunkSectionCache) returns null when
 * {@code section == null || section.hasOnlyAir()}, before retrieveChunkMeshAppenders.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public class LevelSliceMixin implements SectionGeometry.TerrainView, IClientCoreHint {
    @Unique private @Nullable BlockPos hbm$coreHint;

    @Override
    public @Nullable BlockPos hbm$coreHint() {
        return hbm$coreHint;
    }

    @Override
    public void hbm$coreHint(@Nullable BlockPos pos) {
        hbm$coreHint = pos;
    }

    @ModifyExpressionValue(
            method = "prepare",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z"))
    private static boolean hbm$includeGeometry(
            boolean empty,
            @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) SectionPos section) {
        return empty && !SectionGeometry.has(level, section.asLong());
    }
}
