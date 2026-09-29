// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.compat.iris;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.jspecify.annotations.Nullable;

/**
 * Stand-in for Iris' {@code net.irisshaders.iris.shadows.ShadowRenderingState} (the only Iris class
 * the rest of the tree still calls). Iris is not a compile dependency of the 1.21.1 backport, so the
 * call is resolved reflectively: first the same Iris-internal class (present in Iris 1.8.x for
 * 1.21.1), then the public {@code IrisApi.isRenderingShadowPass()}. Without Iris it answers false.
 */
public final class ShadowRenderingState {
    private static final @Nullable MethodHandle QUERY = find();

    private ShadowRenderingState() {}

    public static boolean areShadowsCurrentlyBeingRendered() {
        if (QUERY == null) return false;
        try {
            return (boolean) QUERY.invoke();
        } catch (Throwable t) {
            return false;
        }
    }

    private static @Nullable MethodHandle find() {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            Class<?> state = Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState");
            return lookup.findStatic(
                    state, "areShadowsCurrentlyBeingRendered", MethodType.methodType(boolean.class));
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return lookup.findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class))
                    .bindTo(instance);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return null;
    }
}
