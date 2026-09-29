// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** 26.x {@code net.minecraft.client.model.EntityModel<S extends EntityRenderState>}. */
public abstract class EntityModel<S extends EntityRenderState> extends Model<S> {
    public static final float MODEL_Y_OFFSET = -1.501F;

    protected EntityModel(ModelPart root) {
        this(root, RenderType::entityCutoutNoCull);
    }

    protected EntityModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(root, renderType);
    }
}
