// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.item.EntityMinecartTest;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import com.hbm.backport.client.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.block.state.BlockState;

// backport: 1.21.1's TntMinecartRenderer is immediate (renderMinecartContents); swell and white
// flash follow 1.21.1 TntMinecartRenderer. 26.x flips a flipped cart by swapping its rail end points
// (turning the whole cart); 1.21.1 computes those inside MinecartRenderer.render, so only the
// (asymmetric) bomb is turned around here.
public class RenderMinecartTest extends TntMinecartRenderer
        implements ConcurrentRenderStateExtraction {

    public static final RenderType TYPE =
            RenderTypes.entityCutoutCull(ResourceManager.bomb_boy_tex);

    public RenderMinecartTest(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderMinecartContents(
            MinecartTNT entity,
            float partialTicks,
            BlockState blockState,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int lightCoords) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        poseStack.translate(0.5F, 0.5F, -0.5F);
        if (entity instanceof EntityMinecartTest test && test.isFlipped())
            poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        int fuse = entity.getFuse();
        if (fuse > -1 && fuse - partialTicks + 1.0F < 10.0F) {
            float swell = Mth.clamp(1.0F - (fuse - partialTicks + 1.0F) / 10.0F, 0.0F, 1.0F);
            swell *= swell;
            swell *= swell;
            float scale = 1.0F + swell * 0.3F;
            poseStack.scale(scale, scale, scale);
        }
        int overlay =
                fuse > -1 && fuse / 5 % 2 == 0
                        ? OverlayTexture.pack(OverlayTexture.u(1F), 10)
                        : OverlayTexture.NO_OVERLAY;
        ResourceManager.lil_boy.render(
                poseStack.last(), buffers.getBuffer(TYPE), lightCoords, -1, overlay);
    }
}
