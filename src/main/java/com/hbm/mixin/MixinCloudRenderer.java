// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.client.CloudLayers;
import com.hbm.backport.ARGB;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 26.x has a separate CloudRenderer; 1.21.1 renders clouds in LevelRenderer. The
 * {@link CloudLayers} hooks map onto it as:
 *
 * <ul>
 *   <li>CloudRenderer.render TAIL -> renderClouds TAIL (the vanilla path: a dimension's own cloud
 *       renderer returns early, like 26.x's check for the vanilla CloudRenderer class). The 26.x
 *       arguments are taken from what renderClouds uses: the level's cloud colour, the cloud
 *       status option, the dimension's cloud height (no layers when it is NaN, which draws no
 *       clouds), the render distance in chunks as the range (1.21.1 has no cloud range option),
 *       the camera position, the game time and the partial tick;
 *   <li>CloudRenderer.apply (cloud texture reload) -> onResourceManagerReload TAIL;
 *   <li>CloudRenderer.markForRebuild -> allChanged TAIL (where 1.21.1 regenerates its clouds);
 *   <li>CloudRenderer.endFrame -> renderLevel TAIL;
 *   <li>CloudRenderer.close -> LevelRenderer.close TAIL.
 * </ul>
 */
@Mixin(LevelRenderer.class)
public class MixinCloudRenderer {
    @Shadow private @Nullable ClientLevel level;

    @Inject(
            method = "renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("TAIL"))
    private void hbm$addonLayers(
            PoseStack poseStack,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTicks,
            double camX,
            double camY,
            double camZ,
            CallbackInfo ci) {
        ClientLevel level = this.level;
        if (level == null) return;
        float bottomY = level.effects().getCloudHeight();
        if (Float.isNaN(bottomY)) return;
        Minecraft minecraft = Minecraft.getInstance();
        CloudStatus status = minecraft.options.getCloudsType();
        Vec3 c = level.getCloudColor(partialTicks);
        int color = ARGB.colorFromFloat(1.0F, (float) c.x, (float) c.y, (float) c.z);
        CloudLayers.render(
                color,
                status,
                bottomY,
                minecraft.options.getEffectiveRenderDistance(),
                new Vec3(camX, camY, camZ),
                level.getGameTime(),
                partialTicks);
    }

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void hbm$addonReload(ResourceManager manager, CallbackInfo ci) {
        CloudLayers.reload();
    }

    @Inject(method = "allChanged", at = @At("TAIL"))
    private void hbm$addonRebuild(CallbackInfo ci) {
        CloudLayers.markForRebuild();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void hbm$addonEndFrame(CallbackInfo ci) {
        CloudLayers.endFrame();
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void hbm$addonClose(CallbackInfo ci) {
        CloudLayers.close();
    }
}
