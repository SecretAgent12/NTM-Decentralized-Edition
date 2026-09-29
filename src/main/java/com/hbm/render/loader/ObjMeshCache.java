// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.loader;

import com.hbm.lib.Library;
import java.io.IOException;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ObjMeshCache implements PreparableReloadListener {

    public static final ResourceLocation ID = Library.id("obj_meshes");
    public static final ObjMeshCache INSTANCE = new ObjMeshCache();
    private static volatile IdentityHashMap<
                    ResourceManager, ConcurrentHashMap<ResourceLocation, GroupObject[]>>
            caches = new IdentityHashMap<>();

    private ObjMeshCache() {}

    static GroupObject[] load(ResourceManager resources, ResourceLocation id) {
        var generation = caches;
        ConcurrentHashMap<ResourceLocation, GroupObject[]> cache;
        synchronized (generation) {
            cache = generation.computeIfAbsent(resources, unused -> new ConcurrentHashMap<>());
        }
        GroupObject[] shared =
                cache.computeIfAbsent(
                        id,
                        obj -> {
                            ResourceLocation compiled = ObjMesh.resource(obj);
                            try (InputStream input =
                                    resources.getResourceOrThrow(compiled).open()) {
                                return ObjMesh.read(input, compiled.toString());
                            } catch (IOException e) {
                                throw new IllegalStateException(
                                        "Failed to load mesh " + compiled, e);
                            }
                        });
        GroupObject[] groups = new GroupObject[shared.length];
        for (int i = 0; i < groups.length; i++) groups[i] = shared[i].copy();
        return groups;
    }

    // backport: 1.21.1 reload listeners have no shared-state preparation step; the 26.x
    // prepareSharedState cache reset runs when the reload reaches this listener instead (the cache is
    // keyed by ResourceManager, so listeners loading meshes earlier in the same reload are unaffected)
    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier preparationBarrier,
            ResourceManager resourceManager,
            ProfilerFiller preparationsProfiler,
            ProfilerFiller reloadProfiler,
            Executor backgroundExecutor,
            Executor gameExecutor) {
        caches = new IdentityHashMap<>();
        return preparationBarrier.wait(null);
    }
}
