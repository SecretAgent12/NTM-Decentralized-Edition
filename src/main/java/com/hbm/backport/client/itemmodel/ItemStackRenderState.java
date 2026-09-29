// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.blockmodel.BakedQuad;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.item.ItemStackRenderState}: the resolved layers of one
 * item stack (baked quads with tints/foil, or a special renderer) plus their transforms.
 * {@link #submit} draws them through the core topic's SubmitNodeCollector (immediate in 1.21.1).
 */
public class ItemStackRenderState {
    public ItemDisplayContext displayContext = ItemDisplayContext.NONE;
    public int activeLayerCount;
    private boolean animated;
    private boolean oversizedInGui;
    private @Nullable AABB cachedModelBoundingBox;
    public LayerRenderState[] layers = new LayerRenderState[] {new LayerRenderState()};
    private final List<Object> modelIdentityElements = new ArrayList<>();

    public void ensureCapacity(int requestedLayerCount) {
        int required = activeLayerCount + requestedLayerCount;
        if (required > layers.length) {
            int old = layers.length;
            layers = Arrays.copyOf(layers, required);
            for (int i = old; i < layers.length; i++) layers[i] = new LayerRenderState();
        }
    }

    public LayerRenderState newLayer() {
        ensureCapacity(1);
        LayerRenderState layer = layers[activeLayerCount++];
        layer.clear();
        cachedModelBoundingBox = null;
        return layer;
    }

    public void clear() {
        displayContext = ItemDisplayContext.NONE;
        for (int i = 0; i < activeLayerCount; i++) layers[i].clear();
        activeLayerCount = 0;
        animated = false;
        oversizedInGui = false;
        cachedModelBoundingBox = null;
        modelIdentityElements.clear();
    }

    public boolean isEmpty() {
        return activeLayerCount == 0;
    }

    public void setAnimated() {
        animated = true;
    }

    public boolean isAnimated() {
        return animated;
    }

    public void appendModelIdentityElement(Object element) {
        modelIdentityElements.add(element);
    }

    public Object getModelIdentity() {
        return List.copyOf(modelIdentityElements);
    }

    public void setOversizedInGui(boolean oversizedInGui) {
        this.oversizedInGui = oversizedInGui;
    }

    public boolean isOversizedInGui() {
        return oversizedInGui;
    }

    public boolean usesBlockLight() {
        return activeLayerCount > 0 && layers[0].usesBlockLight;
    }

    public @Nullable TextureAtlasSprite pickParticleIcon(RandomSource random) {
        return activeLayerCount == 0 ? null : layers[random.nextInt(activeLayerCount)].particleIcon;
    }

    /** Every layer's extents, in the item's display space (layer transforms applied). */
    public void visitExtents(Consumer<Vector3fc> output) {
        Vector3f scratch = new Vector3f();
        PoseStack pose = new PoseStack();
        for (int i = 0; i < activeLayerCount; i++) {
            LayerRenderState layer = layers[i];
            if (layer.extents == null) continue;
            Vector3fc[] points = layer.extents.get();
            if (points == null || points.length == 0) continue;
            pose.pushPose();
            layer.applyTransform(pose);
            Matrix4f m = pose.last().pose();
            for (Vector3fc p : points) output.accept(m.transformPosition(p, scratch));
            pose.popPose();
        }
    }

    public AABB getModelBoundingBox() {
        if (cachedModelBoundingBox != null) return cachedModelBoundingBox;
        float[] b = {
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
        };
        visitExtents(
                p -> {
                    b[0] = Math.min(b[0], p.x());
                    b[1] = Math.min(b[1], p.y());
                    b[2] = Math.min(b[2], p.z());
                    b[3] = Math.max(b[3], p.x());
                    b[4] = Math.max(b[4], p.y());
                    b[5] = Math.max(b[5], p.z());
                });
        AABB box = b[0] > b[3] ? new AABB(0, 0, 0, 0, 0, 0) : new AABB(b[0], b[1], b[2], b[3], b[4], b[5]);
        cachedModelBoundingBox = box;
        return box;
    }

    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            int outlineColor) {
        for (int i = 0; i < activeLayerCount; i++)
            layers[i].submit(poseStack, collector, lightCoords, overlayCoords, outlineColor);
    }

    /** backport: 1.21.1 entry point — draws the layers straight into a buffer source. */
    public void render(
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int lightCoords,
            int overlayCoords) {
        submit(poseStack, SubmitNodeCollector.immediate(buffers), lightCoords, overlayCoords, 0);
    }

    static boolean leftHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    public enum FoilType {
        NONE,
        STANDARD,
        SPECIAL
    }

    public class LayerRenderState {
        private static final Vector3fc[] NO_EXTENTS = new Vector3fc[0];
        private static final Supplier<Vector3fc[]> NO_EXTENTS_SUPPLIER = () -> NO_EXTENTS;

        private final List<BakedQuad> quads = new ArrayList<>();
        boolean usesBlockLight;
        @Nullable TextureAtlasSprite particleIcon;
        ItemTransform transform = ItemTransform.NO_TRANSFORM;
        @Nullable Matrix4fc localTransform;
        public FoilType foilType = FoilType.NONE;
        private final IntList tintLayers = new IntArrayList();
        public @Nullable SpecialModelRenderer<Object> specialRenderer;
        public @Nullable Object argumentForSpecialRendering;
        @Nullable Supplier<Vector3fc[]> extents = NO_EXTENTS_SUPPLIER;

        public void clear() {
            quads.clear();
            usesBlockLight = false;
            particleIcon = null;
            transform = ItemTransform.NO_TRANSFORM;
            localTransform = null;
            foilType = FoilType.NONE;
            tintLayers.clear();
            specialRenderer = null;
            argumentForSpecialRendering = null;
            extents = NO_EXTENTS_SUPPLIER;
        }

        public List<BakedQuad> prepareQuadList() {
            return quads;
        }

        public List<BakedQuad> quads() {
            return quads;
        }

        public IntList tintLayers() {
            return tintLayers;
        }

        public void setUsesBlockLight(boolean usesBlockLight) {
            this.usesBlockLight = usesBlockLight;
        }

        public boolean usesBlockLight() {
            return usesBlockLight;
        }

        public void setParticleIcon(@Nullable TextureAtlasSprite particleIcon) {
            this.particleIcon = particleIcon;
        }

        public void setTransform(ItemTransform transform) {
            this.transform = transform;
        }

        public ItemTransform transform() {
            return transform;
        }

        public void setLocalTransform(@Nullable Matrix4fc localTransform) {
            this.localTransform = localTransform;
        }

        public void setExtents(Supplier<Vector3fc[]> extents) {
            this.extents = extents;
        }

        public void setFoilType(FoilType foilType) {
            this.foilType = foilType;
        }

        @SuppressWarnings("unchecked")
        public <T> void setupSpecialModel(SpecialModelRenderer<T> renderer, @Nullable T argument) {
            this.specialRenderer = (SpecialModelRenderer<Object>) renderer;
            this.argumentForSpecialRendering = argument;
        }

        /**
         * Display transform (left-hand aware) x local transform x the -0.5 recentering.
         * backport: unverified: order of the 26.x local transform relative to the display transform
         * (taken as applied inside the display transform, before recentering).
         */
        public void applyTransform(PoseStack poseStack) {
            transform.apply(leftHand(displayContext), poseStack);
            if (localTransform != null) poseStack.mulPose(new Matrix4f(localTransform));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        public void applyTransform(PoseStack.Pose pose) {
            PoseStack stack = SubmitNodeCollector.poseStackOf(pose);
            applyTransform(stack);
            pose.pose().set(stack.last().pose());
            pose.normal().set(stack.last().normal());
        }

        void submit(
                PoseStack poseStack,
                SubmitNodeCollector collector,
                int lightCoords,
                int overlayCoords,
                int outlineColor) {
            poseStack.pushPose();
            applyTransform(poseStack);
            if (specialRenderer != null) {
                specialRenderer.submit(
                        argumentForSpecialRendering,
                        poseStack,
                        collector,
                        lightCoords,
                        overlayCoords,
                        foilType != FoilType.NONE,
                        outlineColor);
            } else if (!quads.isEmpty()) {
                collector.submitItem(
                        poseStack,
                        displayContext,
                        lightCoords,
                        overlayCoords,
                        outlineColor,
                        tintLayers.toIntArray(),
                        quads,
                        foilType);
            }
            poseStack.popPose();
        }
    }
}
