// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.itemmodel;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.special.SpecialModelRenderer}: immediate-style drawing
 * of an item layer (the 26.x successor of BEWLR). The pose it receives already carries the layer's
 * display transform, local transform and the -0.5 recentering.
 */
public interface SpecialModelRenderer<T> {
    void submit(
            @Nullable T argument,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor);

    void getExtents(Consumer<Vector3fc> output);

    default @Nullable T extractArgument(ItemStack stack) {
        return null;
    }

    interface Unbaked<T> {
        MapCodec<? extends Unbaked<T>> type();

        @Nullable SpecialModelRenderer<T> bake(ItemModel.BakingContext context);
    }
}
