// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;

// backport: 1.21.1 CreeperRenderer hooks take the entity; the swelling 26.x reads from
// CreeperRenderState is Creeper.getSwelling(partialTick)
public class RenderCreeperUniversal extends CreeperRenderer
        implements ConcurrentRenderStateExtraction {

    private final ResourceLocation texture;
    private final float swellMod;

    public RenderCreeperUniversal(
            EntityRendererProvider.Context context, ResourceLocation texture, float swellMod) {
        super(context);
        this.texture = texture;
        this.swellMod = swellMod;
    }

    @Override
    protected void scale(Creeper creeper, PoseStack poseStack, float partialTick) {
        float swell = creeper.getSwelling(partialTick);
        float wobble = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
        swell = Mth.clamp(swell, 0.0F, 1.0F);
        swell *= swell;
        swell *= swell;
        swell *= swellMod;
        poseStack.scale(
                (1.0F + swell * 0.4F) * wobble,
                (1.0F + swell * 0.1F) / wobble,
                (1.0F + swell * 0.4F) * wobble);
    }

    @Override
    public ResourceLocation getTextureLocation(Creeper creeper) {
        return texture;
    }
}
