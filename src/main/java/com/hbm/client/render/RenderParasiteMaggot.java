// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.hbm.backport.client.core.VanillaMobRenderer;
import net.minecraft.resources.ResourceLocation;

// backport: vanilla 1.21.1 SilverfishModel is entity-posed: drawn by the vanilla MobRenderer.
public final class RenderParasiteMaggot
        extends VanillaMobRenderer<
                EntityParasiteMaggot, ParasiteMaggotRenderState, SilverfishModel<EntityParasiteMaggot>>
        implements ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/entity/parasite_maggot.png");

    public RenderParasiteMaggot(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    public ParasiteMaggotRenderState createRenderState() {
        return new ParasiteMaggotRenderState();
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteMaggotRenderState state) {
        return TEXTURE;
    }
}
