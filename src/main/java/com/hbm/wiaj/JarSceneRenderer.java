// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj;

import com.hbm.particle.DebrisMesh;
import com.hbm.wiaj.actors.ISpecialActor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.backport.client.gui.render.pip.PictureInPictureRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.ChunkSectionLayer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderTypes;

public final class JarSceneRenderer extends PictureInPictureRenderer<JarRenderState> {
    private JarScript cachedScript;
    private int cachedRevision = -1;
    private DebrisMesh[] cachedBlocks;
    private DebrisMesh[] cachedFluids;
    private int guiScale;

    @Override
    public Class<JarRenderState> getRenderStateClass() {
        return JarRenderState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "NTM Cannery scene";
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2F;
    }

    @Override
    protected void renderToTexture(
            JarRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        // backport: 26.x captured guiScale in a prepare() override; read it from the window instead so
        // the renderer does not depend on the picture-in-picture shim's prepare signature
        guiScale = (int) net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        JarScript script = state.script();
        WorldInAJar world = script.world;
        script.interp = script.isPaused() || script.currentScene == null ? 0F : state.partialTick();

        pose.translate(0, 0, -400D / (10D * guiScale));
        pose.scale(-1, -1, 0.5F);
        double zoom = script.zoom();
        pose.scale((float) zoom, (float) zoom, (float) zoom);
        pose.mulPose(Axis.XP.rotationDegrees((float) script.pitch()));
        pose.mulPose(Axis.YP.rotationDegrees((float) script.yaw()));
        pose.translate(world.sizeX / -2D, -world.sizeY / 2D, world.sizeZ / -2D);
        pose.translate(script.offsetX(), script.offsetY(), script.offsetZ());

        if (cachedScript != script || cachedRevision != world.revision()) {
            // backport: 26.x BlockModelLighter.clearCache -> the 1.21.1 AO/light cache of ModelBlockRenderer
            com.hbm.backport.client.blockmodel.ModelBlockRenderer.clearCache();
            cachedBlocks = DebrisMesh.bake(world, world.sizeX, world.sizeY, world.sizeZ);
            cachedFluids = DebrisMesh.bakeFluids(world, world.sizeX, world.sizeY, world.sizeZ);
            cachedScript = script;
            cachedRevision = world.revision();
        }
        ChunkSectionLayer[] layers = ChunkSectionLayer.values();
        for (int i = 0; i < layers.length; i++) {
            DebrisMesh mesh = cachedBlocks[i];
            if (mesh != null)
                collector.submitCustomGeometry(pose, layerType(layers[i]), mesh::emit);
            DebrisMesh fluid = cachedFluids[i];
            if (fluid != null)
                collector.submitCustomGeometry(pose, layerType(layers[i]), fluid::emit);
        }

        JarRenderContext context = new JarRenderContext(world, pose, collector);
        for (ISpecialActor actor : script.actors.values()) {
            pose.pushPose();
            actor.drawBackgroundComponent(context, script.ticksElapsed, script.interp);
            pose.popPose();
        }
    }

    private static RenderType layerType(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }
}
