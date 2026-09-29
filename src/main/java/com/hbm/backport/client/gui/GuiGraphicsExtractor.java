// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import com.hbm.backport.client.gui.state.GuiRenderState;
import com.hbm.backport.client.rendertype.RenderPipeline;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.gui.GuiGraphicsExtractor} on 1.21.1.
 *
 * <p>26.x screens and HUD layers extract their GUI into a render state that is drawn later; 1.21.1
 * draws immediately through {@link GuiGraphics}. This class IS a 1.21.1 GuiGraphics (so it can be
 * handed to vanilla widgets and NeoForge APIs) that shares the pose stack, scissor stack and buffer
 * source of the vanilla GuiGraphics it is made for ({@link #of}), and adds the 26.x drawing calls
 * the tree makes: {@code blit(RenderPipeline, ...)}, {@code text}, {@code item}, {@code
 * itemDecorations}, {@code setTooltipForNextFrame}, {@code entity}, {@code guiRenderState}, and a
 * {@link GuiPoseStack} pose with the 26.x 2D matrix calls.
 *
 * <p>Z: 26.x has no GUI z; everything here is drawn at the z of the current pose, like 1.21.1's
 * own blits, so 1.21.1's depth-tested item icons (z 100-250) stay in front of later 2D draws.
 */
public class GuiGraphicsExtractor extends GuiGraphics {

    private static final Field POSE = field("pose");
    private static final Field SCISSOR = field("scissorStack");
    private static final Method TOOLTIP_INTERNAL;

    static {
        Method m = null;
        try {
            m = GuiGraphics.class.getDeclaredMethod(
                    "renderTooltipInternal",
                    Font.class,
                    List.class,
                    int.class,
                    int.class,
                    ClientTooltipPositioner.class);
            m.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            m = null;
        }
        TOOLTIP_INTERNAL = m;
    }

    private static @Nullable GuiGraphics lastBase;
    private static @Nullable GuiGraphicsExtractor last;

    /** 26.x {@code graphics.guiRenderState}: element / picture-in-picture submission, drawn now. */
    public final GuiRenderState guiRenderState;

    private final GuiGraphics base;
    private final GuiPoseStack pose;
    private @Nullable Runnable deferredTooltip;
    private int deferDepth;

    protected GuiGraphicsExtractor(GuiGraphics base) {
        super(Minecraft.getInstance(), base.bufferSource());
        this.base = base;
        this.pose = new GuiPoseStack(base.pose());
        try {
            POSE.set(this, pose);
            SCISSOR.set(this, SCISSOR.get(base));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("backport: cannot share the GuiGraphics pose/scissor", e);
        }
        this.guiRenderState = new GuiRenderState(this);
    }

    /**
     * The extractor for a vanilla GuiGraphics (the one it was made for is reused, so a frame's
     * layers and screen share one; a GuiGraphicsExtractor is returned as is).
     */
    public static GuiGraphicsExtractor of(GuiGraphics graphics) {
        if (graphics instanceof GuiGraphicsExtractor e) return e;
        if (graphics == lastBase && last != null) return last;
        GuiGraphicsExtractor e = new GuiGraphicsExtractor(graphics);
        lastBase = graphics;
        last = e;
        return e;
    }

    private static Field field(String name) {
        try {
            Field f = GuiGraphics.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("backport: GuiGraphics." + name + " not found", e);
        }
    }

    /** The vanilla GuiGraphics this extractor draws through. */
    public GuiGraphics vanilla() {
        return base;
    }

    @Override
    public GuiPoseStack pose() {
        return pose;
    }

    // ---- blit ---------------------------------------------------------------------------------

    public void blit(
            RenderPipeline pipeline,
            ResourceLocation texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        blit(pipeline, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight, -1);
    }

    public void blit(
            RenderPipeline pipeline,
            ResourceLocation texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            int color) {
        blit(pipeline, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight, color);
    }

    public void blit(
            RenderPipeline pipeline,
            ResourceLocation texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int uWidth,
            int vHeight,
            int textureWidth,
            int textureHeight) {
        blit(pipeline, texture, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight, -1);
    }

    public void blit(
            RenderPipeline pipeline,
            ResourceLocation texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int uWidth,
            int vHeight,
            int textureWidth,
            int textureHeight,
            int color) {
        quad(
                GuiPipelines.of(pipeline),
                texture,
                x,
                x + width,
                y,
                y + height,
                u / textureWidth,
                (u + uWidth) / textureWidth,
                v / textureHeight,
                (v + vHeight) / textureHeight,
                color);
    }

    /** 26.x {@code blit(texture, x0, y0, x1, y1, u0, u1, v0, v1)}: GUI_TEXTURED, explicit UVs. */
    public void blit(
            ResourceLocation texture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
        quad(GuiPipelines.GUI_TEXTURED, texture, x0, x1, y0, y1, u0, u1, v0, v1, -1);
    }

    public void blitSprite(RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
        blitSprite(pipeline, sprite, x, y, width, height, -1);
    }

    public void blitSprite(
            RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        GuiPipelines p = GuiPipelines.of(pipeline);
        // backport: 1.21.1 blitSprite (nine-slice/tile scaling) takes no color; tint via shader color
        setColor(r(color), g(color), b(color), a(color));
        p.begin();
        blitSprite(sprite, x, y, width, height);
        p.end();
        setColor(1F, 1F, 1F, 1F);
    }

    public void blitSprite(
            RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, float alpha) {
        blitSprite(pipeline, sprite, x, y, width, height, ((int) (alpha * 255F) << 24) | 0xFFFFFF);
    }

    public void blitSprite(
            RenderPipeline pipeline,
            ResourceLocation sprite,
            int textureWidth,
            int textureHeight,
            int u,
            int v,
            int x,
            int y,
            int width,
            int height) {
        GuiPipelines p = GuiPipelines.of(pipeline);
        p.begin();
        blitSprite(sprite, textureWidth, textureHeight, u, v, x, y, width, height);
        p.end();
    }

    public void blitSprite(
            RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height) {
        blitSprite(pipeline, sprite, x, y, width, height, -1);
    }

    public void blitSprite(
            RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
        quad(
                GuiPipelines.of(pipeline),
                sprite.atlasLocation(),
                x,
                x + width,
                y,
                y + height,
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1(),
                color);
    }

    /** One textured quad in the current pose, drawn now (1.21.1 innerBlit with pipeline state). */
    public void quad(
            GuiPipelines pipeline,
            @Nullable ResourceLocation texture,
            float x0,
            float x1,
            float y0,
            float y1,
            float u0,
            float u1,
            float v0,
            float v1,
            int color) {
        if (pipeline.textured && texture != null) RenderSystem.setShaderTexture(0, texture);
        pipeline.begin();
        Matrix4f m = pose.last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, pipeline.format());
        if (pipeline.textured) {
            b.addVertex(m, x0, y0, 0F).setUv(u0, v0).setColor(color);
            b.addVertex(m, x0, y1, 0F).setUv(u0, v1).setColor(color);
            b.addVertex(m, x1, y1, 0F).setUv(u1, v1).setColor(color);
            b.addVertex(m, x1, y0, 0F).setUv(u1, v0).setColor(color);
        } else {
            b.addVertex(m, x0, y0, 0F).setColor(color);
            b.addVertex(m, x0, y1, 0F).setColor(color);
            b.addVertex(m, x1, y1, 0F).setColor(color);
            b.addVertex(m, x1, y0, 0F).setColor(color);
        }
        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        pipeline.end();
    }

    // ---- fill ---------------------------------------------------------------------------------

    public void fill(RenderPipeline pipeline, int x0, int y0, int x1, int y1, int color) {
        GuiPipelines p = GuiPipelines.of(pipeline);
        if (p == GuiPipelines.GUI) {
            fill(x0, y0, x1, y1, color);
            return;
        }
        if (x0 < x1) {
            int t = x0;
            x0 = x1;
            x1 = t;
        }
        if (y0 < y1) {
            int t = y0;
            y0 = y1;
            y1 = t;
        }
        flush();
        GuiPipelines untextured = p.untextured();
        quad(untextured, null, x0, x1, y0, y1, 0F, 0F, 0F, 0F, color);
    }

    // ---- text ---------------------------------------------------------------------------------

    // backport: unverified: 26.x skips text whose color has alpha 0 (1.21.1 Font would make it opaque)
    private static boolean invisible(int color) {
        return (color >>> 24) == 0;
    }

    public void text(Font font, @Nullable String text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

    public void text(Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
        if (text == null || invisible(color)) return;
        drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if (invisible(color)) return;
        drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, Component text, int x, int y, int color) {
        text(font, text, x, y, color, true);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (invisible(color)) return;
        drawString(font, text, x, y, color, shadow);
    }

    public void centeredText(Font font, String text, int x, int y, int color) {
        if (invisible(color)) return;
        drawCenteredString(font, text, x, y, color);
    }

    public void centeredText(Font font, Component text, int x, int y, int color) {
        if (invisible(color)) return;
        drawCenteredString(font, text, x, y, color);
    }

    public void centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
        if (invisible(color)) return;
        drawCenteredString(font, text, x, y, color);
    }

    public void textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color) {
        if (invisible(color)) return;
        drawWordWrap(font, text, x, y, width, color);
    }

    public void textWithBackdrop(Font font, Component text, int x, int y, int width, int color) {
        drawStringWithBackdrop(font, text, x, y, width, color);
    }

    // ---- items --------------------------------------------------------------------------------

    /**
     * 26.x layers GUI draws in submission order; 1.21.1 depth-tests 3D item/entity renders (z
     * 100-250) against everything drawn later at z 0. Clearing the depth buffer after such a
     * render lets later 2D draws cover it, as in 26.x.
     */
    public void resetDepth() {
        flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    public void item(ItemStack stack, int x, int y) {
        renderItem(stack, x, y);
        resetDepth();
    }

    public void item(ItemStack stack, int x, int y, int seed) {
        renderItem(stack, x, y, seed);
        resetDepth();
    }

    public void item(LivingEntity entity, ItemStack stack, int x, int y, int seed) {
        renderItem(entity, stack, x, y, seed);
        resetDepth();
    }

    public void fakeItem(ItemStack stack, int x, int y) {
        renderFakeItem(stack, x, y);
        resetDepth();
    }

    public void fakeItem(ItemStack stack, int x, int y, int seed) {
        renderFakeItem(stack, x, y, seed);
        resetDepth();
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        renderItemDecorations(font, stack, x, y);
        resetDepth();
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String text) {
        renderItemDecorations(font, stack, x, y, text);
        resetDepth();
    }

    // ---- entity -------------------------------------------------------------------------------

    /**
     * 26.x {@code entity(renderState, scale, translation, rotation, overrideCameraAngle, x0, y0, x1,
     * y1)} for the entity itself: rendered now, scissored to the rectangle, centred in it, the way
     * 1.21.1's InventoryScreen.renderEntityInInventory does for living entities.
     */
    public void entity(
            Entity entity,
            float scale,
            Vector3fc translation,
            Quaternionfc rotation,
            @Nullable Quaternionfc overrideCameraAngle,
            int x0,
            int y0,
            int x1,
            int y1) {
        // 26.x composites the entity (rendered offscreen) over what is below it
        resetDepth();
        enableScissor(x0, y0, x1, y1);
        PoseStack ps = pose;
        ps.pushPose();
        ps.translate((x0 + x1) / 2F, (y0 + y1) / 2F, 50F);
        ps.scale(scale, scale, -scale);
        ps.translate(translation.x(), translation.y(), translation.z());
        ps.mulPose(new Quaternionf(rotation));
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (overrideCameraAngle != null) {
            dispatcher.overrideCameraOrientation(
                    new Quaternionf(overrideCameraAngle).conjugate().rotateY((float) Math.PI));
        }
        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(
                () -> dispatcher.render(entity, 0D, 0D, 0D, 0F, 1F, ps, bufferSource(), 0xF000F0));
        flush();
        dispatcher.setRenderShadow(true);
        ps.popPose();
        Lighting.setupFor3DItems();
        disableScissor();
        resetDepth();
    }

    // ---- tooltips -----------------------------------------------------------------------------

    /**
     * While a bridged 26.x screen renders, tooltips are deferred to the end of its frame (26.x
     * draws them last, above everything); elsewhere they are drawn right away.
     */
    public void beginDeferredTooltips() {
        deferDepth++;
    }

    /** Ends a deferral started by {@link #beginDeferredTooltips} and draws the pending tooltip. */
    public void endDeferredTooltips() {
        if (deferDepth > 0) deferDepth--;
        if (deferDepth == 0) renderDeferredTooltip();
    }

    public void renderDeferredTooltip() {
        Runnable r = deferredTooltip;
        deferredTooltip = null;
        if (r != null) r.run();
    }

    private void tooltip(Runnable draw, boolean focused) {
        if (deferDepth == 0) {
            draw.run();
            return;
        }
        // backport: unverified: 26.x keeps the first tooltip of a frame unless a later one is focused
        if (deferredTooltip == null || focused) deferredTooltip = draw;
    }

    public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
        tooltip(() -> renderTooltip(font, stack, x, y), false);
    }

    public void setTooltipForNextFrame(Font font, Component text, int x, int y) {
        tooltip(() -> renderTooltip(font, text, x, y), false);
    }

    public void setTooltipForNextFrame(Font font, List<FormattedCharSequence> lines, int x, int y) {
        tooltip(() -> renderTooltip(font, lines, x, y), false);
    }

    public void setTooltipForNextFrame(
            Font font, Component text, int x, int y, @Nullable ResourceLocation background) {
        setTooltipForNextFrame(font, text, x, y);
    }

    public void setTooltipForNextFrame(
            Font font,
            List<FormattedCharSequence> lines,
            ClientTooltipPositioner positioner,
            int x,
            int y,
            boolean focused) {
        tooltip(() -> renderTooltip(font, lines, positioner, x, y), focused);
    }

    public void setTooltipForNextFrame(
            Font font, List<Component> lines, Optional<TooltipComponent> image, int x, int y) {
        tooltip(() -> renderTooltip(font, lines, image, x, y), false);
    }

    public void setTooltipForNextFrame(
            Font font,
            List<Component> lines,
            Optional<TooltipComponent> image,
            int x,
            int y,
            @Nullable ResourceLocation background) {
        setTooltipForNextFrame(font, lines, image, x, y);
    }

    public void setTooltipForNextFrame(
            Font font,
            List<Component> lines,
            Optional<TooltipComponent> image,
            ItemStack stack,
            int x,
            int y) {
        tooltip(() -> renderTooltip(font, lines, image, stack, x, y), false);
    }

    /**
     * 26.x full form. The tooltip background sprite (26.x tooltip styles) has no 1.21.1 equivalent:
     * 1.21.1 draws its one (NeoForge-event-colourable) tooltip frame.
     */
    public void setTooltipForNextFrame(
            Font font,
            List<FormattedCharSequence> lines,
            Optional<TooltipComponent> image,
            ClientTooltipPositioner positioner,
            int x,
            int y,
            boolean focused,
            @Nullable ResourceLocation background) {
        tooltip(() -> {
            List<ClientTooltipComponent> parts = new ArrayList<>(lines.size() + 1);
            for (FormattedCharSequence line : lines) parts.add(ClientTooltipComponent.create(line));
            // 1.21.1 puts the tooltip image after the first line (Screen.getTooltipFromItem order)
            image.ifPresent(i -> parts.add(parts.isEmpty() ? 0 : 1, ClientTooltipComponent.create(i)));
            renderTooltipComponents(font, parts, x, y, positioner);
        }, focused);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
        tooltip(() -> renderComponentTooltip(font, lines, x, y), false);
    }

    public void setComponentTooltipForNextFrame(
            Font font, List<Component> lines, int x, int y, @Nullable ResourceLocation background) {
        setComponentTooltipForNextFrame(font, lines, x, y);
    }

    /** Draws prepared tooltip components now (1.21.1 GuiGraphics.renderTooltipInternal). */
    public void renderTooltipComponents(
            Font font, List<ClientTooltipComponent> parts, int x, int y, ClientTooltipPositioner positioner) {
        if (parts.isEmpty()) return;
        if (TOOLTIP_INTERNAL != null) {
            try {
                TOOLTIP_INTERNAL.invoke(this, font, parts, x, y, positioner);
                return;
            } catch (ReflectiveOperationException e) {
                // fall through
            }
        }
        List<FormattedCharSequence> text = new ArrayList<>();
        renderTooltip(font, text, positioner == null ? DefaultTooltipPositioner.INSTANCE : positioner, x, y);
    }

    // ---- misc ---------------------------------------------------------------------------------

    private static float a(int c) {
        return (c >>> 24) / 255F;
    }

    private static float r(int c) {
        return (c >> 16 & 0xFF) / 255F;
    }

    private static float g(int c) {
        return (c >> 8 & 0xFF) / 255F;
    }

    private static float b(int c) {
        return (c & 0xFF) / 255F;
    }
}
