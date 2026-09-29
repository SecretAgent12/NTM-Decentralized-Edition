// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

/**
 * 26.x {@code net.minecraft.client.renderer.BindGroupLayouts}. backport: unverified: the constant set is
 * the one the tree uses plus the obvious neighbours; the uniform block names are descriptive.
 */
public final class BindGroupLayouts {
    public static final BindGroupLayout GLOBALS = BindGroupLayout.uniforms("globals", "Globals");
    public static final BindGroupLayout MATRICES_PROJECTION =
            BindGroupLayout.uniforms("matrices_projection", "DynamicTransforms", "Projection");
    public static final BindGroupLayout FOG = BindGroupLayout.uniforms("fog", "Fog");
    public static final BindGroupLayout LIGHT_DIRECTIONS = BindGroupLayout.uniforms("light_directions", "Lighting");
    public static final BindGroupLayout SAMPLER0 = BindGroupLayout.samplers("sampler0", "Sampler0");
    public static final BindGroupLayout SAMPLER1 = BindGroupLayout.samplers("sampler1", "Sampler1");
    public static final BindGroupLayout SAMPLER2 = BindGroupLayout.samplers("sampler2", "Sampler2");
    public static final BindGroupLayout SAMPLER0_SAMPLER2 =
            BindGroupLayout.samplers("sampler0_sampler2", "Sampler0", "Sampler2");
    public static final BindGroupLayout SAMPLER0_SAMPLER1 =
            BindGroupLayout.samplers("sampler0_sampler1", "Sampler0", "Sampler1");
    public static final BindGroupLayout SAMPLER0_SAMPLER1_SAMPLER2 =
            BindGroupLayout.samplers("sampler0_sampler1_sampler2", "Sampler0", "Sampler1", "Sampler2");

    private BindGroupLayouts() {}
}
