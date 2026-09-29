// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import dev.engine_room.flywheel.api.visual.SectionTrackedVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * backport: Flywheel 1.0's AbstractBlockEntityVisual plus CrankShaft's getRenderBoundingBox, which decides the light
 * sections the visual registers for relight (Flywheel 1.0 registers only the block's own section).
 */
public abstract class AbstractBlockEntityVisual<T extends BlockEntity> extends dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual<T> {
    protected AbstractBlockEntityVisual(VisualizationContext ctx, T blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
    }

    /** The level-space bounding box this visual occupies; drives which light sections it registers for relight. */
    protected AABB getRenderBoundingBox() {
        return new AABB(pos);
    }

    @Override
    public void setSectionCollector(SectionTrackedVisual.SectionCollector sectionCollector) {
        this.lightSections = sectionCollector;
        AABB bb = getRenderBoundingBox();
        if (Double.isInfinite(bb.minX) || Double.isInfinite(bb.minY) || Double.isInfinite(bb.minZ)
                || Double.isInfinite(bb.maxX) || Double.isInfinite(bb.maxY) || Double.isInfinite(bb.maxZ)) {
            lightSections.sections(LongSet.of(SectionPos.asLong(pos)));
            return;
        }
        int minSx = SectionPos.blockToSectionCoord(Mth.floor(bb.minX));
        int minSy = SectionPos.blockToSectionCoord(Mth.floor(bb.minY));
        int minSz = SectionPos.blockToSectionCoord(Mth.floor(bb.minZ));
        int maxSx = SectionPos.blockToSectionCoord(Mth.ceil(bb.maxX) - 1);
        int maxSy = SectionPos.blockToSectionCoord(Mth.ceil(bb.maxY) - 1);
        int maxSz = SectionPos.blockToSectionCoord(Mth.ceil(bb.maxZ) - 1);
        int count = (maxSx - minSx + 1) * (maxSy - minSy + 1) * (maxSz - minSz + 1);
        LongSet sections = new LongArraySet(count);
        for (int sx = minSx; sx <= maxSx; sx++)
            for (int sy = minSy; sy <= maxSy; sy++)
                for (int sz = minSz; sz <= maxSz; sz++) sections.add(SectionPos.asLong(sx, sy, sz));
        lightSections.sections(sections);
    }
}
