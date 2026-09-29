// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 26.x {@code net.minecraft.client.renderer.ShaderDefines}: preprocessor defines of a pipeline. backport:
 * 1.21.1 shader programs have no defines; {@link RenderTypeFactory} turns the ones the mod's pipelines use
 * (ALPHA_CUTOUT, EMISSIVE, NO_OVERLAY, NO_CARDINAL_LIGHTING, APPLY_TEXTURE_MATRIX, IS_GRAYSCALE,
 * MIN_RIBBON_WIDTH, RIBBON_FILTER_PADDING) into uniforms of the backport programs.
 */
public final class ShaderDefines {
    public static final ShaderDefines EMPTY = new ShaderDefines(Map.of(), Set.of());

    private final Map<String, String> values;
    private final Set<String> flags;

    public ShaderDefines(Map<String, String> values, Set<String> flags) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.flags = Collections.unmodifiableSet(new LinkedHashSet<>(flags));
    }

    public Map<String, String> values() {
        return values;
    }

    public Set<String> flags() {
        return flags;
    }

    public boolean isEmpty() {
        return values.isEmpty() && flags.isEmpty();
    }

    public boolean has(String name) {
        return flags.contains(name) || values.containsKey(name);
    }

    /** The numeric value of a valued define, or {@code fallback} when absent or not a number. */
    public float floatValue(String name, float fallback) {
        String v = values.get(name);
        if (v == null) return fallback;
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public ShaderDefines withOverrides(ShaderDefines overrides) {
        if (overrides.isEmpty()) return this;
        Map<String, String> v = new LinkedHashMap<>(values);
        Set<String> f = new LinkedHashSet<>(flags);
        v.putAll(overrides.values);
        f.addAll(overrides.flags);
        return new ShaderDefines(v, f);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ShaderDefines d && d.values.equals(values) && d.flags.equals(flags);
    }

    @Override
    public int hashCode() {
        return values.hashCode() * 31 + flags.hashCode();
    }

    @Override
    public String toString() {
        return "ShaderDefines" + values + flags;
    }

    public static final class Builder {
        private final Map<String, String> values = new LinkedHashMap<>();
        private final Set<String> flags = new LinkedHashSet<>();

        Builder() {}

        public Builder define(String name) {
            flags.add(name);
            return this;
        }

        public Builder define(String name, String value) {
            values.put(name, value);
            return this;
        }

        public Builder define(String name, int value) {
            return define(name, Integer.toString(value));
        }

        public Builder define(String name, float value) {
            return define(name, Float.toString(value));
        }

        public Builder addAll(ShaderDefines defines) {
            values.putAll(defines.values);
            flags.addAll(defines.flags);
            return this;
        }

        public ShaderDefines build() {
            return new ShaderDefines(values, flags);
        }
    }
}
