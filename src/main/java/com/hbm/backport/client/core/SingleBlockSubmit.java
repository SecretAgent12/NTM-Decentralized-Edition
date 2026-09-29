// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A block model drawn as an item-like single block with a fixed light (26.x BlockModelRenderState
 * submit; 1.21.1 BlockRenderDispatcher.renderSingleBlock), one custom-geometry node per render type.
 */
public final class SingleBlockSubmit {
    private SingleBlockSubmit() {}

    public static void submit(
            OrderedSubmitNodeCollector collector,
            PoseStack poseStack,
            BlockState state,
            int lightCoords,
            int overlayCoords) {
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) return;
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        int color = mc.getBlockColors().getColor(state, null, null, 0);
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        for (RenderType chunkType : model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY)) {
            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypeHelper.getEntityRenderType(chunkType, false),
                    (pose, buffer) ->
                            dispatcher
                                    .getModelRenderer()
                                    .renderModel(
                                            pose, buffer, state, model, r, g, b, lightCoords,
                                            overlayCoords, ModelData.EMPTY, chunkType));
        }
    }
}
