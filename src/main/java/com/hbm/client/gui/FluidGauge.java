// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.lib.Library;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.gui.render.TextureSetup;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.backport.client.gui.state.BlitRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public final class FluidGauge {

    private static final Map<Fluid, ResourceLocation> SHEETS = new ConcurrentHashMap<>();

    private FluidGauge() {}

    public static @Nullable ResourceLocation sheet(Fluid fluid) {
        if (!NTMFluidProperties.isOwn(fluid)) return null;
        return SHEETS.computeIfAbsent(
                fluid,
                own -> Library.id("textures/gui/fluids/" + NTMFluids.spritePath(own) + ".png"));
    }

    public static void vertical(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, Fluid type) {
        ResourceLocation own = sheet(type);
        if (own != null) {
            graphics.blit(own, x, y, x + width, y + height, 0F, width / 16F, 1F - height / 16F, 1F);
            return;
        }
        tileForeign(graphics, type, x, y, width, height, false, true, false);
    }

    public static void hanging(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, Fluid type) {
        ResourceLocation own = sheet(type);
        if (own != null) {
            graphics.blit(own, x, y, x + width, y + height, 0F, width / 16F, 1F, 1F - height / 16F);
            return;
        }
        tileForeign(graphics, type, x, y, width, height, false, false, true);
    }

    public static void horizontal(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, Fluid type) {
        ResourceLocation own = sheet(type);
        if (own != null) {
            graphics.blit(own, x, y, x + width, y + height, 1F, 1F - width / 16F, 0F, height / 16F);
            return;
        }
        tileForeign(graphics, type, x, y, width, height, true, false, false);
    }

    public static void still(
            GuiGraphicsExtractor graphics, Fluid type, int x, int y, int width, int height) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, stillSprite(type), x, y, width, height, tint(type));
    }

    // backport: 26.x reads the still sprite and tint from the fluid's FluidModel; 1.21.1 has them on
    // the fluid type's client extensions (the same sprite the 1.21.1 fluid renderer uses)
    private static TextureAtlasSprite stillSprite(Fluid type) {
        ResourceLocation still = IClientFluidTypeExtensions.of(type).getStillTexture();
        return Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(still);
    }

    private static int tint(Fluid type) {
        return IClientFluidTypeExtensions.of(type).getTintColor();
    }

    private static void tileForeign(
            GuiGraphicsExtractor graphics,
            Fluid type,
            int x,
            int y,
            int width,
            int height,
            boolean mirrorU,
            boolean anchorBottom,
            boolean flipV) {
        TextureAtlasSprite sprite = stillSprite(type);
        int color = tint(type);

        AbstractTexture atlas =
                Minecraft.getInstance().getTextureManager().getTexture(sprite.atlasLocation());
        TextureSetup setup = TextureSetup.singleTexture(atlas);
        Matrix3x2f pose = graphics.pose().matrix2d();

        for (int px = 0; px < width; px += 16) {
            int tileW = Math.min(16, width - px);
            float u0 = sprite.getU(mirrorU ? 1F : 0F);
            float u1 = sprite.getU(mirrorU ? 1F - tileW / 16F : tileW / 16F);
            for (int py = 0; py < height; py += 16) {
                int tileH = Math.min(16, height - py);
                float v0 = sprite.getV(flipV ? 1F : anchorBottom ? 1F - tileH / 16F : 0F);
                float v1 = sprite.getV(flipV ? 1F - tileH / 16F : anchorBottom ? 1F : tileH / 16F);
                int ty = anchorBottom ? y + height - py - tileH : y + py;
                graphics.guiRenderState.addGuiElement(
                        new BlitRenderState(
                                RenderPipelines.GUI_TEXTURED,
                                setup,
                                pose,
                                x + px,
                                ty,
                                x + px + tileW,
                                ty + tileH,
                                u0,
                                u1,
                                v0,
                                v1,
                                color,
                                null));
            }
        }
    }
}
