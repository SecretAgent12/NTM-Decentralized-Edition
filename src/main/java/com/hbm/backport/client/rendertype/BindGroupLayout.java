// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 26.x {@code com.mojang.blaze3d.pipeline.BindGroupLayout}: the samplers and uniform blocks a pipeline
 * binds. backport: descriptive only; 1.21.1 binds samplers through the RenderType's texture, overlay
 * and lightmap shards, and uniforms through the ShaderInstance.
 */
public record BindGroupLayout(String name, List<String> samplers, List<String> uniformBlocks) {

    public static BindGroupLayout samplers(String name, String... samplers) {
        return new BindGroupLayout(name, List.of(samplers), List.of());
    }

    public static BindGroupLayout uniforms(String name, String... blocks) {
        return new BindGroupLayout(name, List.of(), List.of(blocks));
    }

    /** All sampler names of the given layouts, in binding order, without duplicates. */
    public static List<String> flattenSamplers(Collection<BindGroupLayout> layouts) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (BindGroupLayout layout : layouts) out.addAll(layout.samplers());
        return new ArrayList<>(out);
    }

    public static List<String> flattenUniformBlocks(Collection<BindGroupLayout> layouts) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (BindGroupLayout layout : layouts) out.addAll(layout.uniformBlocks());
        return new ArrayList<>(out);
    }
}
