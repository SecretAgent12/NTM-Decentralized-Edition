// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.RenderConfig;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.util.GameTime;
import com.hbm.backport.client.rendertype.PrimitiveTopology;
import com.hbm.backport.client.rendertype.BlendFunction;
import com.hbm.backport.client.rendertype.ColorTargetState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.hbm.backport.client.rendertype.BlendFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import com.hbm.backport.client.rendertype.BindGroupLayouts;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.core.SkyRenderState;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import com.hbm.backport.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

public final class NTMSkybox {
    private static final ResourceLocation DIGAMMA_STAR = Library.id("textures/misc/star_digamma.png");
    private static final ResourceLocation LODE_STAR = Library.id("textures/misc/star_lode.png");
    private static final ResourceLocation BOBMAZON_SAT = Library.id("textures/misc/sat_bobmazon.png");

    public static final RenderPipeline IMPACT_STARS =
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withLocation(Library.id("pipeline/impact_stars"))
                    .withVertexShader("core/stars")
                    .withFragmentShader("core/stars")
                    .withColorTargetState(
                            new ColorTargetState(
                                    new BlendFunction(
                                            BlendFactor.SRC_ALPHA,
                                            BlendFactor.ONE_MINUS_SRC_ALPHA,
                                            BlendFactor.ONE,
                                            BlendFactor.ZERO)))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .build();
    private static Mode mode = Mode.NONE;
    private static float dustAlpha;
    private static float rainAlpha;
    private static float sunAngle;
    private static boolean renderLodeStar;
    private static long lastStarCheck;

    private NTMSkybox() {}

    // backport: 1.21.1 draws the sprites immediately (Tesselator), no GPU buffer to release
    public static void close() {}

    public static void extract(ClientLevel level, SkyRenderState state) {
        mode = Mode.NONE;
        if (!RenderConfig.skyboxes || !state.overworldSky) return;

        float dust = ImpactWorldHandler.getDustForClient(level);
        if (dust > 0 || ImpactWorldHandler.getFireForClient(level) > 0) {
            mode = Mode.IMPACT;
            dustAlpha = Math.max(1 - dust * 2, 0);
            state.rainBrightness *= dustAlpha;
            state.sunriseAndSunsetColor = ARGB.color(0, state.sunriseAndSunsetColor);
        } else if (level.dimension() == Level.OVERWORLD) {
            mode = Mode.CHAINLOADER;
        } else {
            return;
        }
        rainAlpha = state.rainBrightness;
        sunAngle = state.sunAngle;
    }

    public static boolean impact() {
        return mode == Mode.IMPACT;
    }

    public static boolean lodeStarVisible() {
        return renderLodeStar;
    }

    public static float starAlpha() {
        return rainAlpha;
    }

    public static void poseImpactStars(PoseStack pose, float starAngle) {
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotation(starAngle));
        pose.mulPose(Axis.YP.rotationDegrees(-19.0F));
    }

    /**
     * backport: 1.21.1's model-view stack holds no camera rotation during level rendering; the caller
     * (the sky hook) passes the view rotation LevelRenderer.renderSky receives.
     */
    public static void renderExtras(Matrix4f viewRotation) {
        if (mode == Mode.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        float brightness = Mth.sin(sunAngle * 0.5F);
        brightness *= brightness;
        float starAlpha = mode == Mode.IMPACT ? dustAlpha : 1.0F;
        float satAlpha = mode == Mode.IMPACT ? rainAlpha : 1.0F;
        float lodeSize = 0.5F + player.level().getRandom().nextFloat() * 0.25F;
        PoseStack pose = new PoseStack();
        pose.last().pose().set(viewRotation);

        if (mode == Mode.CHAINLOADER && renderLodeStar) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-75.0F));
            pose.mulPose(Axis.YP.rotationDegrees(10.0F));
            draw(LODE_STAR, pose, lodeSize, 100.0F, 1.0F, 1.0F);
            pose.popPose();
        }

        float digamma = (float) HbmLivingProps.getDigamma(player);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotation(sunAngle));
        pose.mulPose(Axis.XP.rotationDegrees(140.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-40.0F));
        draw(
                DIGAMMA_STAR,
                pose,
                1.0F + digamma * 0.25F,
                100.0F - digamma * 2.5F,
                brightness,
                starAlpha);
        pose.popPose();

        long now = GameTime.now();
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(-40.0F));
        pose.mulPose(Axis.YP.rotationDegrees(now % 360_000L / 1000.0F));
        pose.mulPose(Axis.XP.rotationDegrees(now % 36_000L / 100.0F));
        draw(BOBMAZON_SAT, pose, 0.5F, 100.0F, brightness, satAlpha);
        pose.popPose();
    }

    // backport: 26.x draws a cached quad through RenderPipelines.CELESTIAL in a render pass; 1.21.1
    // draws it immediately with the position-tex shader and 1.21.1's celestial blending (renderSky)
    private static void draw(
            ResourceLocation texture,
            PoseStack pose,
            float size,
            float distance,
            float brightness,
            float alpha) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(pose.last().pose());
        modelView.translate(0.0F, distance, 0.0F);
        modelView.scale(size, 1.0F, size);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);
        BufferBuilder builder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-1.0F, 0.0F, -1.0F).setUv(0.0F, 0.0F);
        builder.addVertex(1.0F, 0.0F, -1.0F).setUv(0.0F, 1.0F);
        builder.addVertex(1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F);
        builder.addVertex(-1.0F, 0.0F, 1.0F).setUv(1.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!RenderConfig.skyboxes || mc.level == null) return;
        long millis = GameTime.millis();
        if (lastStarCheck + 200 >= millis) return;
        renderLodeStar = false;
        lastStarCheck = millis;

        LocalPlayer player = mc.player;
        if (player == null) return;

        Vec3 from = player.position();
        Vec3 heading = new Vec3(0, 0, -1).xRot((float) Math.toRadians(-15)).scale(25);
        BlockHitResult hit =
                mc.level.clip(
                        new ClipContext(
                                from,
                                from.add(heading),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                player));
        renderLodeStar =
                hit.getType() == HitResult.Type.BLOCK
                        && mc.level
                                .getBlockState(hit.getBlockPos())
                                .is(ModBlocks.GLASS_POLARIZED.get());
    }

    private enum Mode {
        NONE,
        CHAINLOADER,
        IMPACT
    }
}
