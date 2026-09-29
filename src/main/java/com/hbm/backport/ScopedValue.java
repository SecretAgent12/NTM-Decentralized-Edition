// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

/**
 * java.lang.ScopedValue (final in Java 25, preview in Java 21) as the tree uses it:
 * a value bound for the duration of a call on the current thread. Backed by a
 * ThreadLocal, restored after the call, so nesting behaves the same.
 */
public final class ScopedValue<T> {

    private final ThreadLocal<Object> slot = new ThreadLocal<>();
    private static final Object UNBOUND = new Object();

    private ScopedValue() {}

    public static <T> ScopedValue<T> newInstance() {
        return new ScopedValue<>();
    }

    public boolean isBound() {
        Object v = slot.get();
        return v != null && v != UNBOUND;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (!isBound()) throw new NoSuchElementException("ScopedValue not bound");
        return (T) slot.get();
    }

    public T orElse(T other) {
        return isBound() ? get() : other;
    }

    public static <T> Carrier where(ScopedValue<T> key, T value) {
        return new Carrier(key, value);
    }

    public static final class Carrier {
        private final ScopedValue<?> key;
        private final Object value;

        private Carrier(ScopedValue<?> key, Object value) {
            this.key = key;
            this.value = value;
        }

        public void run(Runnable op) {
            Object prev = key.slot.get();
            key.slot.set(value);
            try {
                op.run();
            } finally {
                key.slot.set(prev == null ? UNBOUND : prev);
            }
        }

        public <R> R call(Callable<? extends R> op) throws Exception {
            Object prev = key.slot.get();
            key.slot.set(value);
            try {
                return op.call();
            } finally {
                key.slot.set(prev == null ? UNBOUND : prev);
            }
        }

        public <R> R get(java.util.function.Supplier<? extends R> op) {
            Object prev = key.slot.get();
            key.slot.set(value);
            try {
                return op.get();
            } finally {
                key.slot.set(prev == null ? UNBOUND : prev);
            }
        }
    }
}
