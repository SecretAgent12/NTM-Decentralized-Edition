// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.core.LivingEntityRenderState;
import com.hbm.backport.client.core.VanillaMobRenderer;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// backport: the 1.21.1 ChickenModel is entity-posed (wing flap read from the entity via
// getBob), so the vanilla MobRenderer draws it; 26.x's ChickenRenderState flap fields are not needed.
public class RenderDuck<T extends EntityDuck>
        extends VanillaMobRenderer<T, LivingEntityRenderState, ChickenModel<T>>
        implements ConcurrentRenderStateExtraction {

    private final float scale;

    private RenderDuck(EntityRendererProvider.Context context, float shadow, float scale) {
        super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), shadow);
        this.scale = scale;
    }

    public static RenderDuck<EntityDuck> duck(EntityRendererProvider.Context context) {
        return new RenderDuck<>(context, 0.3F, 1F);
    }

    public static RenderDuck<EntityQuackos> quackos(EntityRendererProvider.Context context) {
        return new RenderDuck<>(context, 7.5F, 25F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    // 1.21.1 ChickenRenderer.getBob: the wing angle the vanilla ChickenModel reads as ageInTicks
    @Override
    protected float getBob(T entity, float partialTicks) {
        float flap = net.minecraft.util.Mth.lerp(partialTicks, entity.oFlap, entity.flap);
        float flapSpeed = net.minecraft.util.Mth.lerp(partialTicks, entity.oFlapSpeed, entity.flapSpeed);
        return (net.minecraft.util.Mth.sin(flap) + 1.0F) * flapSpeed;
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        if (scale != 1F) poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(LivingEntityRenderState state) {
        return ResourceManager.duck_tex;
    }
}
