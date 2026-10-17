// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.capability.ResolvedCapCache;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import com.hbm.world.NtmWorldgenFields;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

// backport: injected interface methods are default (throwing): javac only resolves
// an interface method on a concrete class read from bytecode if the method is not
// abstract, which is the same rule Loom and ModDevGradle interface injection state.
public interface IServerLevelExtension {

    default EndpointRegistry hbm$endpoints() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default LevelNodeGraph<?>[] hbm$graphs() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default void hbm$graphs(LevelNodeGraph<?>[] graphs) {
        throw new AssertionError("backport: implemented by mixin");
    }
    default Long2IntOpenHashMap hbm$capGraphChunks() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default void hbm$redrawCapabilityShapes() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default ResolvedCapCache hbm$resolvedCaps() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default LongOpenHashSet hbm$publishedChunks() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default PhysicalGraph.@Nullable Batch hbm$physicalBatch() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default void hbm$physicalBatch(PhysicalGraph.@Nullable Batch batch) {
        throw new AssertionError("backport: implemented by mixin");
    }
    default @Nullable FlushIndex hbm$flushIndex() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default void hbm$flushIndex(FlushIndex index) {
        throw new AssertionError("backport: implemented by mixin");
    }
    default NtmWorldgenFields hbm$worldgenFields() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default Set<Entity> hbm$radarTargets() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default Set<LivingEntity> hbm$livingTargets() {
        throw new AssertionError("backport: implemented by mixin");
    }
    default Set<Entity> hbm$storageEntities() {
        throw new AssertionError("backport: implemented by mixin");
    }
}
