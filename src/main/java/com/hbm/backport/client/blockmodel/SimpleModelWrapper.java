// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import java.util.List;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** 26.x net.minecraft.client.resources.model.SimpleModelWrapper: a part over a QuadCollection. */
public record SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, Material.Baked particleMaterial)
        implements BlockStateModelPart {

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return quads.getQuads(direction);
    }

    @Override
    public int materialFlags() {
        return quads.materialFlags();
    }

    /** 26.x SimpleModelWrapper.bake(baker, id, state): a model's top geometry as one part. */
    public static SimpleModelWrapper bake(ModelBaker baker, net.minecraft.resources.ResourceLocation id, ModelState state) {
        ResolvedModel model = baker.getModel(id);
        TextureSlots slots = model.getTopTextureSlots();
        return new SimpleModelWrapper(
                model.bakeTopGeometry(slots, baker, state),
                model.getTopAmbientOcclusion(),
                model.resolveParticleMaterial(slots, baker));
    }
}
