// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.backport.client.blockmodel.BlockStateModel;
import com.hbm.backport.client.blockmodel.SingleVariant;
import com.hbm.backport.client.blockmodel.ModelBaker;
import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.hbm.backport.client.blockmodel.ResolvedModel;
import com.hbm.backport.client.blockmodel.SimpleModelWrapper;
import com.hbm.backport.client.blockmodel.QuadCollection;
import com.hbm.backport.client.blockmodel.TextureSlots;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public interface CarrierRoot extends BlockStateModel.UnbakedRoot {

    ResourceLocation carrier();

    QuadCollection quads(
            BlockState state, ModelBaker baker, ResolvedModel carrier, TextureSlots slots);

    default void resolveExtraDependencies(ResolvableModel.Resolver resolver) {}

    @Override
    default void resolveDependencies(ResolvableModel.Resolver resolver) {
        resolver.markDependency(carrier());
        resolveExtraDependencies(resolver);
    }

    @Override
    default BlockStateModel bake(BlockState state, ModelBaker baker) {
        ResolvedModel carrier = baker.getModel(carrier());
        TextureSlots slots = carrier.getTopTextureSlots();
        return new SingleVariant(
                new SimpleModelWrapper(
                        quads(state, baker, carrier, slots),
                        carrier.getTopAmbientOcclusion(),
                        carrier.resolveParticleMaterial(slots, baker)));
    }
}
