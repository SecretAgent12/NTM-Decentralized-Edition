// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 26.x {@code net.minecraft.client.model.Model<S>}: a model posed from a render state. Built on the
 * 1.21.1 {@link net.minecraft.client.model.Model} so it still renders through 1.21.1 APIs.
 */
public abstract class Model<S> extends net.minecraft.client.model.Model {
    protected final ModelPart root;
    private final List<ModelPart> allParts;

    public Model(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
        this.root = root;
        this.allParts = root.getAllParts().toList();
    }

    public final ModelPart root() {
        return root;
    }

    public final List<ModelPart> allParts() {
        return allParts;
    }

    public Optional<ModelPart> getAnyDescendantWithName(String name) {
        // backport: unverified: 26.x looks the name up across all descendants
        if (name.equals("root")) return Optional.of(root);
        return root.getAllParts()
                .filter(p -> p.hasChild(name))
                .findFirst()
                .map(p -> p.getChild(name));
    }

    public void setupAnim(S state) {
        resetPose();
    }

    public final void resetPose() {
        for (ModelPart part : allParts) part.resetPose();
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
        root.render(poseStack, buffer, lightCoords, overlayCoords, color);
    }

    /** 26.x Model.Simple: a stateless model around a root part. */
    public static class Simple extends Model<Object> {
        public Simple(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
            super(root, renderType);
        }
    }
}
