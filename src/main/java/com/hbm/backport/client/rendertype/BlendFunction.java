// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

/**
 * 26.x {@code com.mojang.blaze3d.pipeline.BlendFunction}: separate color and alpha blend equations.
 * backport: unverified: component names {@code color()/alpha()} and {@code sourceFactor()/destFactor()/op()}
 * follow the Iris compat code in the tree.
 */
public record BlendFunction(Equation color, Equation alpha) {

    public record Equation(BlendFactor sourceFactor, BlendFactor destFactor, BlendOp op) {
        public Equation(BlendFactor sourceFactor, BlendFactor destFactor) {
            this(sourceFactor, destFactor, BlendOp.ADD);
        }
    }

    public static final BlendFunction LIGHTNING = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
    public static final BlendFunction GLINT =
            new BlendFunction(BlendFactor.SRC_COLOR, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE);
    public static final BlendFunction OVERLAY =
            new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO);
    public static final BlendFunction ADDITIVE = new BlendFunction(BlendFactor.ONE, BlendFactor.ONE);
    public static final BlendFunction TRANSLUCENT =
            new BlendFunction(
                    BlendFactor.SRC_ALPHA,
                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                    BlendFactor.ONE,
                    BlendFactor.ONE_MINUS_SRC_ALPHA);
    public static final BlendFunction TRANSLUCENT_PREMULTIPLIED_ALPHA =
            new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA);
    public static final BlendFunction INVERT =
            new BlendFunction(
                    BlendFactor.ONE_MINUS_DST_COLOR,
                    BlendFactor.ONE_MINUS_SRC_COLOR,
                    BlendFactor.ONE,
                    BlendFactor.ZERO);
    public static final BlendFunction CRUMBLING =
            new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.SRC_COLOR, BlendFactor.ONE, BlendFactor.ZERO);
    public static final BlendFunction ENTITY_OUTLINE_BLIT =
            new BlendFunction(
                    BlendFactor.SRC_ALPHA,
                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                    BlendFactor.ZERO,
                    BlendFactor.ONE);

    public BlendFunction(BlendFactor source, BlendFactor dest) {
        this(new Equation(source, dest), new Equation(source, dest));
    }

    public BlendFunction(
            BlendFactor sourceColor, BlendFactor destColor, BlendFactor sourceAlpha, BlendFactor destAlpha) {
        this(new Equation(sourceColor, destColor), new Equation(sourceAlpha, destAlpha));
    }

    /** backport: applies this function to the 1.21.1 GL state (enables blending). */
    public void apply() {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                color.sourceFactor().gl(), color.destFactor().gl(), alpha.sourceFactor().gl(), alpha.destFactor().gl());
        if (color.op() != BlendOp.ADD) com.mojang.blaze3d.systems.RenderSystem.blendEquation(color.op().gl());
    }

    /** backport: undoes {@link #apply()} (vanilla default: blending off, default function). */
    public void clear() {
        if (color.op() != BlendOp.ADD) com.mojang.blaze3d.systems.RenderSystem.blendEquation(BlendOp.ADD.gl());
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }
}
