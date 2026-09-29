// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.ARGB;
import com.hbm.backport.client.core.SkyRenderState;
import com.hbm.client.render.NTMSkybox;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 26.x SkyRenderer (extractRenderState + renderSunMoonAndStars/renderStars with render
 * pipelines) is 1.21.1 LevelRenderer.renderSky, which computes the sky values inline and draws with
 * RenderSystem state. The 26.x hooks map onto it as:
 *
 * <ul>
 *   <li>extractRenderState TAIL -> renderSky HEAD fills a {@link SkyRenderState} with the values
 *       renderSky is about to use and runs {@link NTMSkybox#extract}; its adjustments (rain
 *       brightness of the sun and moon, sunrise glow alpha) are applied at the matching calls;
 *   <li>renderSunMoonAndStars / renderStars (impact stars: own pose, alpha blending, alpha scaled
 *       by {@link NTMSkybox#starAlpha}) -> the star VertexBuffer draw;
 *   <li>the 26.x sky pass extras (MixinLevelRenderer) -> renderSky's end, when the normal sky was
 *       drawn, with renderSky's view rotation;
 *   <li>SkyRenderer.close -> nothing (NTMSkybox.close is a no-op on 1.21.1).
 * </ul>
 */
@Mixin(LevelRenderer.class)
public abstract class MixinSkyRenderer {
    @Shadow private @Nullable ClientLevel level;

    private static final String RENDER_SKY =
            "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V";

    @Unique private float hbm$sunMoonAlphaScale = 1.0F;
    @Unique private boolean hbm$hideSunrise;
    @Unique private boolean hbm$normalSky;

    @Inject(method = RENDER_SKY, at = @At("HEAD"))
    private void hbm$ntmSkybox(
            Matrix4f viewRotation,
            Matrix4f projection,
            float partialTicks,
            Camera camera,
            boolean foggy,
            Runnable setupFog,
            CallbackInfo ci) {
        hbm$normalSky = false;
        hbm$sunMoonAlphaScale = 1.0F;
        hbm$hideSunrise = false;
        ClientLevel level = this.level;
        if (level == null) return;
        SkyRenderState state = new SkyRenderState();
        state.overworldSky = level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL;
        state.sunAngle = level.getSunAngle(partialTicks);
        state.rainBrightness = 1.0F - level.getRainLevel(partialTicks);
        float[] sunrise =
                level.effects().getSunriseColor(level.getTimeOfDay(partialTicks), partialTicks);
        state.sunriseAndSunsetColor =
                sunrise == null ? 0 : ARGB.colorFromFloat(sunrise[3], sunrise[0], sunrise[1], sunrise[2]);
        state.starBrightness = level.getStarBrightness(partialTicks) * state.rainBrightness;
        float rain = state.rainBrightness;
        int sunriseAlpha = ARGB.alpha(state.sunriseAndSunsetColor);
        NTMSkybox.extract(level, state);
        hbm$sunMoonAlphaScale = rain > 0.0F ? state.rainBrightness / rain : 1.0F;
        hbm$hideSunrise = sunriseAlpha != 0 && ARGB.alpha(state.sunriseAndSunsetColor) == 0;
    }

    @Inject(
            method = RENDER_SKY,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private void hbm$normalSkyDrawn(CallbackInfo ci) {
        hbm$normalSky = true;
    }

    @ModifyExpressionValue(
            method = RENDER_SKY,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F"))
    private float @Nullable [] hbm$impactSunrise(float @Nullable [] color) {
        return hbm$hideSunrise ? null : color;
    }

    // setShaderColor ordinal 2 = (1, 1, 1, 1 - rain) before the sun and moon quads
    @ModifyArg(
            method = RENDER_SKY,
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V",
                            ordinal = 2),
            index = 3)
    private float hbm$impactSunMoonAlpha(float alpha) {
        return alpha * hbm$sunMoonAlphaScale;
    }

    // VertexBuffer.drawWithShader ordinal 1 = the star buffer
    @WrapOperation(
            method = RENDER_SKY,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V",
                            ordinal = 1))
    private void hbm$impactStars(
            VertexBuffer stars,
            Matrix4f pose,
            Matrix4f projection,
            ShaderInstance shader,
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) Matrix4f viewRotation,
            @Local(argsOnly = true) float partialTicks) {
        if (!NTMSkybox.impact() || level == null) {
            original.call(stars, pose, projection, shader);
            return;
        }
        PoseStack impact = new PoseStack();
        impact.mulPose(viewRotation);
        // backport: unverified: 26.x's star angle taken as 1.21.1's sun angle (radians), the angle
        // 1.21.1 turns the stars by
        NTMSkybox.poseImpactStars(impact, level.getSunAngle(partialTicks));
        float[] c = RenderSystem.getShaderColor();
        float r = c[0], g = c[1], b = c[2], a = c[3];
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(r, g, b, a * NTMSkybox.starAlpha());
        original.call(stars, impact.last().pose(), projection, shader);
        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
    }

    @Inject(method = RENDER_SKY, at = @At("TAIL"))
    private void hbm$ntmSkyExtras(
            Matrix4f viewRotation,
            Matrix4f projection,
            float partialTicks,
            Camera camera,
            boolean foggy,
            Runnable setupFog,
            CallbackInfo ci) {
        if (hbm$normalSky) NTMSkybox.renderExtras(viewRotation);
        hbm$normalSky = false;
    }
}
