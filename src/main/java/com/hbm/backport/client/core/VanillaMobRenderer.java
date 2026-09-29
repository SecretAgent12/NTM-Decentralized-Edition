// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * A 1.21.1 {@link net.minecraft.client.renderer.entity.MobRenderer} for mobs drawn with a vanilla
 * 1.21.1 (entity-posed) model, keeping the 26.x render-state hooks of the tree's renderers:
 * createRenderState / extractRenderState / scale(S) / getTextureLocation(S) / getFlipDegrees() and an
 * optional {@link #submit} for geometry drawn after the model. The vanilla renderer poses and draws
 * the model and its layers (as 26.x's state-posed vanilla models would).
 */
public abstract class VanillaMobRenderer<
                T extends Mob, S extends LivingEntityRenderState, M extends net.minecraft.client.model.EntityModel<T>>
        extends net.minecraft.client.renderer.entity.MobRenderer<T, M> {

    private @Nullable S current;

    protected VanillaMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    public abstract S createRenderState();

    /** The state of the mob being rendered (null outside render()). */
    protected @Nullable S currentState() {
        return current;
    }

    public void extractRenderState(T entity, S state, float partialTicks) {}

    private S extract(T entity, float partialTicks, int light) {
        S state = createRenderState();
        EntityRenderState.extractEntity(entity, state, partialTicks, light);
        LivingEntityRenderState.extractLiving(entity, state, partialTicks);
        if (state instanceof HumanoidRenderState humanoid)
            HumanoidRenderState.extractHumanoid(entity, humanoid, partialTicks);
        state.shadowRadius = this.shadowRadius;
        extractRenderState(entity, state, partialTicks);
        return state;
    }

    /** Extra geometry after the model (26.x submit minus the model, which 1.21.1 already drew). */
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {}

    protected void scale(S state, PoseStack poseStack) {}

    public abstract ResourceLocation getTextureLocation(S state);

    protected float getFlipDegrees() {
        return 90.0F;
    }

    @Override
    protected final void scale(T entity, PoseStack poseStack, float partialTick) {
        if (current != null) scale(current, poseStack);
    }

    @Override
    public final ResourceLocation getTextureLocation(T entity) {
        S state = current;
        if (state == null) state = extract(entity, 0.0F, 0xF000F0);
        return getTextureLocation(state);
    }

    @Override
    protected float getFlipDegrees(T entity) {
        return getFlipDegrees();
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        S previous = current;
        S state = extract(entity, partialTick, packedLight);
        current = state;
        try {
            beforeRender(state);
            super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
            submit(state, poseStack, new ImmediateSubmitNodeCollector(buffers), CameraRenderState.current());
        } finally {
            current = previous;
        }
    }

    /** Hook to pick per-state render resources (e.g. swap {@code model}) before the model draws. */
    protected void beforeRender(S state) {}
}
