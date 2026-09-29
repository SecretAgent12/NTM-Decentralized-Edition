// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.core.ImmediateSubmitNodeCollector;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * backport: render state for an item without a 26.x item definition (vanilla and other mods' items),
 * built from its 1.21.1 baked model: overrides resolved, NeoForge perspective transform captured as
 * the layer's local transform, quads per item render type with 1.21.1 ItemColors as tints. Custom
 * renderer models (chest, shield, banner...) become a special layer drawn by their BEWLR.
 */
public final class LegacyItemModel implements ItemModel {
    public static final LegacyItemModel INSTANCE = new LegacyItemModel();

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity living = owner != null ? owner.asLivingEntity() : null;
        BakedModel model = mc.getItemRenderer().getModel(item, level, living, seed);
        output.appendModelIdentityElement(model);

        // NeoForge perspective handling (ClientHooks.handleCameraTransforms) -> one local matrix
        PoseStack capture = new PoseStack();
        BakedModel transformed =
                model.applyTransform(displayContext, capture, ItemStackRenderState.leftHand(displayContext));
        Matrix4f local = new Matrix4f(capture.last().pose());
        ModelRenderProperties properties =
                new ModelRenderProperties(
                        transformed.usesBlockLight(), transformed.getParticleIcon(), transformed.getTransforms());

        ItemStackRenderState.FoilType foil = ItemStackRenderState.FoilType.NONE;
        if (item.hasFoil()) {
            foil =
                    BlockModelWrapper.hasSpecialAnimatedTexture(item)
                            ? ItemStackRenderState.FoilType.SPECIAL
                            : ItemStackRenderState.FoilType.STANDARD;
            output.setAnimated();
        }

        if (transformed.isCustomRenderer()) {
            ItemStackRenderState.LayerRenderState layer = output.newLayer();
            layer.setUsesBlockLight(properties.usesBlockLight());
            layer.setParticleIcon(properties.particleIcon());
            layer.setLocalTransform(local);
            layer.setFoilType(foil);
            layer.setupSpecialModel(BewlrRenderer.INSTANCE, new BewlrArgument(item.copy(), displayContext));
            output.setAnimated();
            return;
        }

        ItemColors colors = mc.getItemColors();
        int start = output.activeLayerCount;
        BlockModelWrapper.appendLayers(
                output, item, displayContext, transformed, properties, new int[0], foil, local);
        for (int i = start; i < output.activeLayerCount; i++) {
            ItemStackRenderState.LayerRenderState layer = output.layers[i];
            // display transform already folded into the local matrix
            layer.setTransform(ItemTransform.NO_TRANSFORM);
            int max = -1;
            for (var quad : layer.prepareQuadList()) max = Math.max(max, quad.materialInfo().tintIndex());
            for (int t = 0; t <= max; t++) layer.tintLayers().add(colors.getColor(item, t));
        }
    }

    public record BewlrArgument(ItemStack stack, ItemDisplayContext context) {}

    /** Draws a 1.21.1 custom-renderer item through its BlockEntityWithoutLevelRenderer. */
    public static final class BewlrRenderer implements SpecialModelRenderer<BewlrArgument> {
        public static final BewlrRenderer INSTANCE = new BewlrRenderer();

        @Override
        public void submit(
                @Nullable BewlrArgument argument,
                PoseStack poseStack,
                SubmitNodeCollector collector,
                int lightCoords,
                int overlayCoords,
                boolean hasFoil,
                int outlineColor) {
            if (argument == null) return;
            // backport: BEWLRs need a buffer source; decorating collectors (mirror/crumbling) are
            // bypassed and the frame's main buffer source is used
            MultiBufferSource buffers =
                    collector instanceof ImmediateSubmitNodeCollector immediate
                            ? immediate.buffers()
                            : Minecraft.getInstance().renderBuffers().bufferSource();
            IClientItemExtensions.of(argument.stack())
                    .getCustomRenderer()
                    .renderByItem(argument.stack(), argument.context(), poseStack, buffers, lightCoords, overlayCoords);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {}
    }
}
