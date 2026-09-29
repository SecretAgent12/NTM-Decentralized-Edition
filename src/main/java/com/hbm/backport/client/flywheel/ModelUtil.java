// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.flywheel;

import com.hbm.lib.crankshaft.CrankShaftMaterials;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.VertexList;
import java.util.Collection;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

/**
 * backport: Flywheel 1.0's ModelUtil plus CrankShaft's layer-keyed item material table. The layer is any enum whose
 * constants are named like 26.x ChunkSectionLayer (SOLID / CUTOUT / TRANSLUCENT). 1.21.1 has one atlas for blocks and
 * items and draws items unmipped, so both atlas flavours map to CrankShaft's *_BLOCK_ITEM materials.
 */
public final class ModelUtil {
    public static final float BOUNDING_SPHERE_EPSILON = 1e-4f;

    private ModelUtil() {}

    public static @Nullable Material getMaterial(RenderType chunkRenderType, boolean shaded) {
        return dev.engine_room.flywheel.lib.model.ModelUtil.getMaterial(chunkRenderType, shaded);
    }

    public static @Nullable Material getMaterial(RenderType chunkRenderType, boolean shaded, boolean ambientOcclusion) {
        return dev.engine_room.flywheel.lib.model.ModelUtil.getMaterial(chunkRenderType, shaded, ambientOcclusion);
    }

    public static @Nullable Material getItemMaterial(RenderType renderType) {
        return dev.engine_room.flywheel.lib.model.ModelUtil.getItemMaterial(renderType);
    }

    public static @Nullable Material getItemMaterial(Enum<?> layer) {
        return getItemMaterial(layer, true);
    }

    public static @Nullable Material getItemMaterial(Enum<?> layer, boolean blocksAtlas) {
        return switch (layer.name()) {
            case "SOLID" -> CrankShaftMaterials.SOLID_BLOCK_ITEM;
            case "CUTOUT", "CUTOUT_MIPPED" -> CrankShaftMaterials.CUTOUT_BLOCK_ITEM;
            case "TRANSLUCENT" -> CrankShaftMaterials.TRANSLUCENT_BLOCK_ITEM;
            default -> null;
        };
    }

    public static int computeTotalVertexCount(Iterable<Mesh> meshes) {
        return dev.engine_room.flywheel.lib.model.ModelUtil.computeTotalVertexCount(meshes);
    }

    public static Vector4f computeBoundingSphere(Collection<Model.ConfiguredMesh> meshes) {
        return dev.engine_room.flywheel.lib.model.ModelUtil.computeBoundingSphere(meshes);
    }

    public static Vector4f computeBoundingSphere(Iterable<Mesh> meshes) {
        return dev.engine_room.flywheel.lib.model.ModelUtil.computeBoundingSphere(meshes);
    }

    public static Vector4f computeBoundingSphere(VertexList vertexList) {
        return dev.engine_room.flywheel.lib.model.ModelUtil.computeBoundingSphere(vertexList);
    }
}
