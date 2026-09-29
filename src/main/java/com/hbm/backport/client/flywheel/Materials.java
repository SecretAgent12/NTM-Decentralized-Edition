// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.hbm.lib.crankshaft.CrankShaftMaterials;
import dev.engine_room.flywheel.api.material.Material;

/** backport: Flywheel 1.0 Materials plus the ones CrankShaft adds ({@link CrankShaftMaterials}). */
public final class Materials {
    private Materials() {}

    public static final Material SOLID_BLOCK = dev.engine_room.flywheel.lib.material.Materials.SOLID_BLOCK;
    public static final Material SOLID_UNSHADED_BLOCK = dev.engine_room.flywheel.lib.material.Materials.SOLID_UNSHADED_BLOCK;
    public static final Material CUTOUT_MIPPED_BLOCK = dev.engine_room.flywheel.lib.material.Materials.CUTOUT_MIPPED_BLOCK;
    public static final Material CUTOUT_MIPPED_UNSHADED_BLOCK = dev.engine_room.flywheel.lib.material.Materials.CUTOUT_MIPPED_UNSHADED_BLOCK;
    public static final Material CUTOUT_BLOCK = dev.engine_room.flywheel.lib.material.Materials.CUTOUT_BLOCK;
    public static final Material CUTOUT_UNSHADED_BLOCK = dev.engine_room.flywheel.lib.material.Materials.CUTOUT_UNSHADED_BLOCK;
    public static final Material TRANSLUCENT_BLOCK = dev.engine_room.flywheel.lib.material.Materials.TRANSLUCENT_BLOCK;
    public static final Material TRANSLUCENT_UNSHADED_BLOCK = dev.engine_room.flywheel.lib.material.Materials.TRANSLUCENT_UNSHADED_BLOCK;
    public static final Material TRIPWIRE_BLOCK = dev.engine_room.flywheel.lib.material.Materials.TRIPWIRE_BLOCK;
    public static final Material TRIPWIRE_UNSHADED_BLOCK = dev.engine_room.flywheel.lib.material.Materials.TRIPWIRE_UNSHADED_BLOCK;
    public static final Material GLINT = dev.engine_room.flywheel.lib.material.Materials.GLINT;
    public static final Material GLINT_ENTITY = dev.engine_room.flywheel.lib.material.Materials.GLINT_ENTITY;
    public static final Material TRANSLUCENT_ENTITY = dev.engine_room.flywheel.lib.material.Materials.TRANSLUCENT_ENTITY;
    public static final Material SOLID_ITEM = CrankShaftMaterials.SOLID_ITEM;
    public static final Material CUTOUT_ITEM = CrankShaftMaterials.CUTOUT_ITEM;
    public static final Material TRANSLUCENT_ITEM = CrankShaftMaterials.TRANSLUCENT_ITEM;
    public static final Material SOLID_BLOCK_ITEM = CrankShaftMaterials.SOLID_BLOCK_ITEM;
    public static final Material CUTOUT_BLOCK_ITEM = CrankShaftMaterials.CUTOUT_BLOCK_ITEM;
    public static final Material TRANSLUCENT_BLOCK_ITEM = CrankShaftMaterials.TRANSLUCENT_BLOCK_ITEM;
    public static final Material CUTOUT = CrankShaftMaterials.CUTOUT;
    public static final Material CUTOUT_NO_CULL = CrankShaftMaterials.CUTOUT_NO_CULL;
    public static final Material CUTOUT_CLIP_SLAB = CrankShaftMaterials.CUTOUT_CLIP_SLAB;
    public static final Material CUTOUT_CLIP_HALFSPACE = CrankShaftMaterials.CUTOUT_CLIP_HALFSPACE;
    public static final Material TRANSLUCENT = CrankShaftMaterials.TRANSLUCENT;
    public static final Material TRANSLUCENT_NO_CULL = CrankShaftMaterials.TRANSLUCENT_NO_CULL;
    public static final Material TRANSLUCENT_NO_DEPTH_WRITE = CrankShaftMaterials.TRANSLUCENT_NO_DEPTH_WRITE;
    public static final Material TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL = CrankShaftMaterials.TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL;
    public static final Material ADDITIVE = CrankShaftMaterials.ADDITIVE;
    public static final Material ADDITIVE_NO_CULL = CrankShaftMaterials.ADDITIVE_NO_CULL;
    public static final Material CRUMBLING = CrankShaftMaterials.CRUMBLING;
}
