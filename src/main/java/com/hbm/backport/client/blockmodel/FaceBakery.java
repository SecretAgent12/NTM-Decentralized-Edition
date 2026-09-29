// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 26.x static FaceBakery.bakeQuad on top of the 1.21.1 FaceBakery (same geometry and UV
 * algorithm; the 1.21.1 int[] quad is decoded into a 26.x {@link BakedQuad}).
 */
public final class FaceBakery {
    private static final net.minecraft.client.renderer.block.model.FaceBakery VANILLA =
            new net.minecraft.client.renderer.block.model.FaceBakery();

    private FaceBakery() {}

    public static BakedQuad bakeQuad(
            @Nullable ModelBaker baker,
            Vector3fc from,
            Vector3fc to,
            CuboidFace face,
            Material.Baked material,
            Direction direction,
            ModelState state,
            @Nullable BlockElementRotation rotation,
            boolean shade,
            int lightEmission) {
        CuboidFace.UVs uvs = face.uvs() != null ? face.uvs() : CuboidFace.defaultUvs(from, to, direction);
        BlockElementFace vanillaFace = new BlockElementFace(
                face.cullForDirection(),
                face.tintIndex(),
                face.texture(),
                new BlockFaceUV(uvs.toArray(), face.rotation().degrees()));
        net.minecraft.client.renderer.block.model.BakedQuad quad = VANILLA.bakeQuad(
                new Vector3f(from), new Vector3f(to), vanillaFace, material.sprite(), direction,
                state.vanilla(), rotation, shade);
        BakedQuad decoded = BakedQuad.fromVanilla(quad);
        BakedQuad.MaterialInfo info = BakedQuad.MaterialInfo.of(
                material, material.transparency(), face.tintIndex(), shade, lightEmission, true);
        return decoded.withMaterialInfo(info);
    }

    public static BakedQuad bakeQuad(
            Vector3fc from,
            Vector3fc to,
            CuboidFace face,
            Material.Baked material,
            Direction direction,
            ModelState state,
            @Nullable BlockElementRotation rotation,
            boolean shade,
            int lightEmission) {
        return bakeQuad(null, from, to, face, material, direction, state, rotation, shade, lightEmission);
    }
}
