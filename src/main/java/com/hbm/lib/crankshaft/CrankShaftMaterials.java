// Ported from CrankShaft (https://github.com/Warfactory-Official/CrankShaft), MIT License.
// Copyright (c) 2026 movblock; portions Copyright (c) 2021-2024 Jozufozu (Flywheel, MIT).
// Modified by SecretAgent12 (NTM 1.21.1 backport): package relocated, ported to Flywheel 1.0 / Minecraft 1.21.1
// SPDX-License-Identifier: MIT (full license text: NOTICE.md in this directory)

package com.hbm.lib.crankshaft;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.api.material.LightShader;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.MaterialShaders;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.lib.material.SimpleCutoutShader;
import dev.engine_room.flywheel.lib.material.SimpleLightShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.SimpleMaterialShaders;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/**
 * The materials, cutout shaders and light shaders CrankShaft adds to Flywheel's Materials / CutoutShaders /
 * LightShaders. backport: items live on the block atlas in 1.21.1 (26.x has a separate item atlas), so the *_ITEM and
 * *_BLOCK_ITEM families coincide apart from mipmapping; clip materials test their plane in a material fragment shader.
 */
public final class CrankShaftMaterials {
    public static final CutoutShader CUTOUT_ONE = new SimpleCutoutShader(rl("cutout/crankshaft/one.glsl"));
    public static final CutoutShader CUTOUT_TINY = new SimpleCutoutShader(rl("cutout/crankshaft/tiny.glsl"));
    // Flywheel 1.0's CutoutShaders.ONE_TENTH / HALF (records: equal instances share the shader index); spelled out so
    // this class does not reference the CutoutShaders name the backport redirects to a facade.
    private static final CutoutShader ONE_TENTH =
            new SimpleCutoutShader(ResourceLocation.fromNamespaceAndPath("flywheel", "cutout/one_tenth.glsl"));
    private static final CutoutShader HALF =
            new SimpleCutoutShader(ResourceLocation.fromNamespaceAndPath("flywheel", "cutout/half.glsl"));
    public static final LightShader LIGHT_NONE = new SimpleLightShader(rl("light/crankshaft/none.glsl"));

    private static final ResourceLocation DEFAULT_VERT = ResourceLocation.fromNamespaceAndPath("flywheel", "material/default.vert");
    public static final MaterialShaders CLIP_SLAB_SHADERS =
            new SimpleMaterialShaders(DEFAULT_VERT, rl("material/crankshaft/clip_slab.frag"));
    public static final MaterialShaders CLIP_HALFSPACE_SHADERS =
            new SimpleMaterialShaders(DEFAULT_VERT, rl("material/crankshaft/clip_halfspace.frag"));

    // Item materials (dropped items, item frames, item displays): the item baker keeps raw quad colours,
    // so directional shading comes from the shader (ENTITY cardinal lighting, the SimpleMaterial default),
    // and light uses the packed instance light, not the block SMOOTH terrain LUT.
    public static final Material SOLID_ITEM = SimpleMaterial.builder()
            .texture(TextureAtlas.LOCATION_BLOCKS)
            .build();
    public static final Material CUTOUT_ITEM = SimpleMaterial.builderOf(SOLID_ITEM)
            .cutout(ONE_TENTH)
            .build();
    public static final Material TRANSLUCENT_ITEM = SimpleMaterial.builderOf(SOLID_ITEM)
            .transparency(Transparency.ORDER_INDEPENDENT)
            .build();
    // mipmap(false): vanilla's item/entity render types bind the atlas unmipped.
    public static final Material SOLID_BLOCK_ITEM = SimpleMaterial.builderOf(SOLID_ITEM)
            .mipmap(false)
            .build();
    public static final Material CUTOUT_BLOCK_ITEM = SimpleMaterial.builderOf(CUTOUT_ITEM)
            .mipmap(false)
            .build();
    public static final Material TRANSLUCENT_BLOCK_ITEM = SimpleMaterial.builderOf(TRANSLUCENT_ITEM)
            .mipmap(false)
            .build();
    public static final Material CUTOUT = SimpleMaterial.builder()
            .cutout(HALF)
            // CHUNK mode matches 1.12.2 ambient-only GL_LIGHTING for entity renders (no
            // directional contribution), whereas ENTITY mode would be too dark on the sides.
            .cardinalLightingMode(CardinalLightingMode.CHUNK)
            .build();
    public static final Material CUTOUT_NO_CULL = SimpleMaterial.builderOf(CUTOUT)
            .backfaceCulling(false)
            .build();
    // backport: CrankShaft's CLIP_SLAB / CLIP_HALFSPACE cutouts = plane test + alpha < 0.1; here the plane test is the
    // material shader and the alpha test the ONE_TENTH cutout. Requires the CLIP_TRANSFORMED instance type.
    public static final Material CUTOUT_CLIP_SLAB = SimpleMaterial.builderOf(CUTOUT)
            .cutout(ONE_TENTH)
            .shaders(CLIP_SLAB_SHADERS)
            .build();
    public static final Material CUTOUT_CLIP_HALFSPACE = SimpleMaterial.builderOf(CUTOUT)
            .cutout(ONE_TENTH)
            .shaders(CLIP_HALFSPACE_SHADERS)
            .build();
    public static final Material TRANSLUCENT = SimpleMaterial.builder()
            .transparency(Transparency.ORDER_INDEPENDENT)
            .build();
    public static final Material TRANSLUCENT_NO_CULL = SimpleMaterial.builderOf(TRANSLUCENT)
            .backfaceCulling(false)
            .build();
    public static final Material TRANSLUCENT_NO_DEPTH_WRITE = SimpleMaterial.builderOf(TRANSLUCENT)
            .writeMask(WriteMask.COLOR)
            .cardinalLightingMode(CardinalLightingMode.OFF)
            .useLight(false)
            .build();
    public static final Material TRANSLUCENT_NO_DEPTH_WRITE_NO_CULL = SimpleMaterial.builderOf(TRANSLUCENT_NO_DEPTH_WRITE)
            .backfaceCulling(false)
            .build();
    public static final Material ADDITIVE = SimpleMaterial.builder()
            .transparency(Transparency.ADDITIVE)
            .writeMask(WriteMask.COLOR)
            .cardinalLightingMode(CardinalLightingMode.OFF)
            .useLight(false)
            .build();
    public static final Material ADDITIVE_NO_CULL = SimpleMaterial.builderOf(ADDITIVE)
            .backfaceCulling(false)
            .build();
    public static final Material CRUMBLING = SimpleMaterial.builder()
            .transparency(Transparency.CRUMBLING)
            .writeMask(WriteMask.COLOR)
            .build();

    private CrankShaftMaterials() {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("hbm", path);
    }
}
