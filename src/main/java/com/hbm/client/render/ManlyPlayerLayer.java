// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.model.Meshes;
import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.lib.Library;
import com.hbm.packet.toclient.PlayerAppearancePayload;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import com.hbm.backport.client.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

// backport: a 1.21.1 player render layer (entity-based, immediate). 26.x reads the appearance flags
// from AvatarRenderState (filled by a mixin); here they come from the player the same way that mixin
// computed them. The glowing outline pass is drawn by 1.21.1's outline buffer source.
public class ManlyPlayerLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE = Library.id("textures/entity/player_fem.png");
    private final HFRWavefrontObject mesh;
    private final int head, body, leftArm, rightArm, leftLeg, rightLeg;

    public ManlyPlayerLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        mesh = Meshes.load(Library.id("models/armor/player_fem.obj")).noSmooth();
        head = mesh.partId("Head");
        body = mesh.partId("Body");
        leftArm = mesh.partId("LeftArm");
        rightArm = mesh.partId("RightArm");
        leftLeg = mesh.partId("LeftLeg");
        rightLeg = mesh.partId("RightLeg");
    }

    private static byte appearance(AbstractClientPlayer player) {
        return player instanceof LocalPlayer local
                ? PlayerAppearancePayload.fromEffects(local)
                : player.hbm$appearance();
    }

    @Override
    public void render(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if ((appearance(player) & PlayerAppearance.MANLY) == 0) return;
        Minecraft mc = Minecraft.getInstance();
        boolean invisible = player.isInvisible();
        boolean invisibleToPlayer = invisible && (mc.player == null || player.isInvisibleTo(mc.player));
        boolean translucent = invisible && !invisibleToPlayer;
        RenderType type =
                translucent
                        ? RenderTypes.entityTranslucentCullItemTarget(TEXTURE)
                        : invisible ? null : WorldRenderPipeline.oneSidedCutout(TEXTURE);
        if (type == null) return;
        int color = translucent ? 654311423 : -1;
        // 1.21.1 LivingEntityRenderer.getOverlayCoords(player, 0)
        int overlay =
                OverlayTexture.pack(
                        OverlayTexture.u(0F), OverlayTexture.v(player.hurtTime > 0 || player.deathTime > 0));
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        limb(pose, buffers, light, model.head, head, 0, 0, type, color, overlay);
        limb(pose, buffers, light, model.body, body, 0, 0, type, color, overlay);
        limb(pose, buffers, light, model.leftArm, leftArm, 5F, 2F, type, color, overlay);
        limb(pose, buffers, light, model.rightArm, rightArm, -5F, 2F, type, color, overlay);
        limb(pose, buffers, light, model.leftLeg, leftLeg, 1.9F, 12F, type, color, overlay);
        limb(pose, buffers, light, model.rightLeg, rightLeg, -1.9F, 12F, type, color, overlay);
    }

    private void limb(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            ModelPart bone,
            int part,
            float originX,
            float originY,
            @Nullable RenderType type,
            int color,
            int overlay) {
        pose.pushPose();
        bone.translateAndRotate(pose);
        pose.translate(-originX / 16F, -originY / 16F, 0);
        pose.scale(1F / 16F, 1F / 16F, 1F / 16F);
        mesh.renderPart(pose.last(), buffers.getBuffer(type), light, color, overlay, part);
        pose.popPose();
    }
}
