// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.client.WeatherTints;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 26.x WeatherEffectRenderer is 1.21.1 LevelRenderer.renderSnowAndRain, which draws the
 * rain and snow columns with the particle shader through RenderSystem. The {@link WeatherTints}
 * tint of the level is claimed at its HEAD; its rain / snow colours replace the white of the rain
 * vertices (the four setColor calls before the SNOW branch) and of the snow vertices (the four
 * after it), keeping vanilla's alpha, as 26.x does in renderInstances. The tint's render pipelines
 * (26.x replacements for WEATHER_DEPTH_WRITE / WEATHER_NO_DEPTH_WRITE) are not applied: 1.21.1 has
 * no weather pipeline to swap.
 */
@Mixin(LevelRenderer.class)
public class MixinWeatherEffectRenderer {
    private static final String RENDER = "renderSnowAndRain";
    private static final String SET_COLOR =
            "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;";
    private static final String SNOW =
            "Lnet/minecraft/world/level/biome/Biome$Precipitation;SNOW:Lnet/minecraft/world/level/biome/Biome$Precipitation;";

    @Unique private WeatherTints.@Nullable Tint hbm$tint;

    @Inject(method = RENDER, at = @At("HEAD"))
    private void hbm$claimTint(CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        hbm$tint = level == null ? null : WeatherTints.tint(level);
    }

    @WrapOperation(
            method = RENDER,
            at = @At(value = "INVOKE", target = SET_COLOR),
            slice = @Slice(to = @At(value = "FIELD", target = SNOW)))
    private VertexConsumer hbm$tintRain(
            VertexConsumer consumer, float r, float g, float b, float a, Operation<VertexConsumer> original) {
        WeatherTints.Tint tint = hbm$tint;
        if (tint == null) return original.call(consumer, r, g, b, a);
        return hbm$tinted(consumer, tint.rain(), a, original);
    }

    @WrapOperation(
            method = RENDER,
            at = @At(value = "INVOKE", target = SET_COLOR),
            slice = @Slice(from = @At(value = "FIELD", target = SNOW)))
    private VertexConsumer hbm$tintSnow(
            VertexConsumer consumer, float r, float g, float b, float a, Operation<VertexConsumer> original) {
        WeatherTints.Tint tint = hbm$tint;
        if (tint == null) return original.call(consumer, r, g, b, a);
        return hbm$tinted(consumer, tint.snow(), a, original);
    }

    @Unique
    private static VertexConsumer hbm$tinted(
            VertexConsumer consumer, int rgb, float a, Operation<VertexConsumer> original) {
        return original.call(
                consumer,
                (rgb >> 16 & 0xFF) / 255.0F,
                (rgb >> 8 & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F,
                a);
    }
}
