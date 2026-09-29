// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.Map;

/**
 * 26.x {@code com.mojang.blaze3d.GpuFormat}: a vertex attribute's component format. backport: builds
 * 1.21.1 {@link VertexFormatElement}s; 26.x {@code VertexFormat.builder(0).addAttribute(name, format)} is
 * rewritten to {@link #formatBuilder()}. Vanilla attribute names map to the vanilla elements; any other
 * name registers one new element (once per name), whose usage picks the 1.21.1 attribute upload path:
 * normalized bytes -> COLOR / NORMAL, integers -> UV (glVertexAttribIPointer), floats -> GENERIC.
 */
public enum GpuFormat {
    R32_FLOAT(VertexFormatElement.Type.FLOAT, 1, false),
    RG32_FLOAT(VertexFormatElement.Type.FLOAT, 2, false),
    RGB32_FLOAT(VertexFormatElement.Type.FLOAT, 3, false),
    RGBA32_FLOAT(VertexFormatElement.Type.FLOAT, 4, false),
    RGBA8_UNORM(VertexFormatElement.Type.UBYTE, 4, true),
    RGBA8_SNORM(VertexFormatElement.Type.BYTE, 4, true),
    RGB8_SNORM(VertexFormatElement.Type.BYTE, 3, true),
    RG16_SINT(VertexFormatElement.Type.SHORT, 2, false),
    RG16_UINT(VertexFormatElement.Type.USHORT, 2, false),
    R32_SINT(VertexFormatElement.Type.INT, 1, false),
    R32_UINT(VertexFormatElement.Type.UINT, 1, false);

    private final VertexFormatElement.Type type;
    private final int count;
    private final boolean normalized;

    GpuFormat(VertexFormatElement.Type type, int count, boolean normalized) {
        this.type = type;
        this.count = count;
        this.normalized = normalized;
    }

    public VertexFormatElement.Type type() {
        return type;
    }

    public int count() {
        return count;
    }

    public boolean normalized() {
        return normalized;
    }

    private static final Map<String, VertexFormatElement> CUSTOM = new HashMap<>();

    /** The 1.21.1 element for an attribute {@code name} of this format. */
    public synchronized VertexFormatElement element(String name) {
        VertexFormatElement vanilla =
                switch (name) {
                    case "Position" -> VertexFormatElement.POSITION;
                    case "Color" -> VertexFormatElement.COLOR;
                    case "UV0" -> VertexFormatElement.UV0;
                    case "UV1" -> VertexFormatElement.UV1;
                    case "UV2" -> VertexFormatElement.UV2;
                    case "Normal" -> VertexFormatElement.NORMAL;
                    default -> null;
                };
        if (vanilla != null && vanilla.type() == type && vanilla.count() == count) return vanilla;
        VertexFormatElement e = CUSTOM.get(name);
        if (e != null) {
            if (e.type() != type || e.count() != count)
                throw new IllegalStateException("backport: attribute " + name + " registered as " + e);
            return e;
        }
        VertexFormatElement.Usage usage;
        if (normalized) usage = type == VertexFormatElement.Type.BYTE ? VertexFormatElement.Usage.NORMAL
                : VertexFormatElement.Usage.COLOR;
        else if (type == VertexFormatElement.Type.FLOAT) usage = VertexFormatElement.Usage.GENERIC;
        else usage = VertexFormatElement.Usage.UV;
        e = VertexFormatElement.register(VertexFormatElement.findNextId(), 0, type, usage, count);
        CUSTOM.put(name, e);
        return e;
    }

    /** 26.x {@code VertexFormat.builder(0)} (buffer binding 0). */
    public static FormatBuilder formatBuilder() {
        return new FormatBuilder();
    }

    public static final class FormatBuilder {
        private final VertexFormat.Builder builder = VertexFormat.builder();

        FormatBuilder() {}

        public FormatBuilder addAttribute(String name, GpuFormat format) {
            builder.add(name, format.element(name));
            return this;
        }

        /** 1.21.1-style element add. */
        public FormatBuilder add(String name, VertexFormatElement element) {
            builder.add(name, element);
            return this;
        }

        public FormatBuilder padding(int bytes) {
            builder.padding(bytes);
            return this;
        }

        public VertexFormat build() {
            return builder.build();
        }
    }
}
