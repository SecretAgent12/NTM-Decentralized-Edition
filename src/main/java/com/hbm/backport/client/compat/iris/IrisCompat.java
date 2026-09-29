// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.compat.iris;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.jspecify.annotations.Nullable;

/**
 * Reflective access to Iris' stable public API ({@code net.irisshaders.iris.api.v0.IrisApi}, the same
 * in Iris 1.8.x for 1.21.1 and later), for renderers that need a fallback while a shader pack is
 * active. backport: the 26.x compat (client/render/iris + mixin/compat/iris) hooked 26.x-only Iris
 * internals and is disabled; see the compat topic report.
 */
public final class IrisCompat {
    private static final @Nullable Object API = api();
    private static final @Nullable MethodHandle PACK_IN_USE = handle("isShaderPackInUse");

    private IrisCompat() {}

    public static boolean isLoaded() {
        return API != null;
    }

    /** True while an Iris shader pack is active (1.21.1 Iris then replaces vanilla core shaders only). */
    public static boolean isShaderPackInUse() {
        if (PACK_IN_USE == null) return false;
        try {
            return (boolean) PACK_IN_USE.invoke();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isRenderingShadowPass() {
        return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }

    private static @Nullable Object api() {
        try {
            return Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static @Nullable MethodHandle handle(String name) {
        if (API == null) return null;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return MethodHandles.publicLookup()
                    .findVirtual(api, name, MethodType.methodType(boolean.class))
                    .bindTo(API);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
