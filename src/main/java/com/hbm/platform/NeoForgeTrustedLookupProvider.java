// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.platform.services.ITrustedLookupProvider;
import java.lang.invoke.MethodHandles;

public final class NeoForgeTrustedLookupProvider implements ITrustedLookupProvider {
    private static final MethodHandles.Lookup IMPL_LOOKUP = bootstrapImplLookup();

    /**
     * backport: NTM: NEXT builds a small bridge class with ASM and defines it in its own class loader
     * to read IMPL_LOOKUP reflectively. CurseForge rejects mods that generate classes at runtime, so
     * DE reads the field through sun.misc.Unsafe only (jdk.unsupported opens sun.misc to everyone;
     * this used to be the fallback). If that fails, callers such as VectorApi fall back to plain Java.
     */
    private static MethodHandles.Lookup bootstrapImplLookup() {
        try {
            return unsafeImplLookup();
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new RuntimeException("Failed to bootstrap IMPL_LOOKUP", e);
        }
    }

    @SuppressWarnings("removal")
    private static MethodHandles.Lookup unsafeImplLookup() throws ReflectiveOperationException {
        java.lang.reflect.Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        java.lang.reflect.Field impl = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        return (MethodHandles.Lookup)
                unsafe.getObject(unsafe.staticFieldBase(impl), unsafe.staticFieldOffset(impl));
    }

    @Override
    public MethodHandles.Lookup implLookup() {
        return IMPL_LOOKUP;
    }
}
