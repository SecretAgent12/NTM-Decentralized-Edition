// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * 26.x {@code net.minecraft.client.renderer.entity.EntityRenderer<T, S>} (extract state, then
 * submit) on the 1.21.1 immediate {@link net.minecraft.client.renderer.entity.EntityRenderer}: the
 * 1.21.1 {@code render} call creates and extracts the state and submits into an {@link
 * ImmediateSubmitNodeCollector}. Shadows, fire and leashes are drawn by 1.21.1 itself (dispatcher /
 * MobRenderer), the name tag by 1.21.1's {@code super.render}.
 */
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState>
        extends net.minecraft.client.renderer.entity.EntityRenderer<T> {

    // packed light 1.21.1 passed to render(), read by extractRenderState (-1 outside render())
    private int pendingLight = -1;

    protected EntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public abstract S createRenderState();

    public final S createRenderState(T entity, float partialTicks) {
        S state = createRenderState();
        extractRenderState(entity, state, partialTicks);
        return state;
    }

    public void extractRenderState(T entity, S state, float partialTicks) {
        EntityRenderState.extractEntity(
                entity,
                state,
                partialTicks,
                pendingLight >= 0 ? pendingLight : this.getPackedLightCoords(entity, partialTicks));
        if (entity instanceof net.minecraft.world.entity.LivingEntity living
                && state instanceof LivingEntityRenderState livingState) {
            LivingEntityRenderState.extractLiving(living, livingState, partialTicks);
            if (livingState instanceof HumanoidRenderState humanoid)
                HumanoidRenderState.extractHumanoid(living, humanoid, partialTicks);
        }
        state.shadowRadius = this.shadowRadius;
        state.passengerOffset = null;
        // backport: the name tag itself is drawn by 1.21.1's super.render (RenderNameTagEvent)
        state.nameTag = this.shouldShowName(entity) ? this.getNameTag(entity) : null;
        state.nameTagAttachment =
                state.nameTag != null
                        ? entity.getAttachments()
                                .getNullable(
                                        net.minecraft.world.entity.EntityAttachment.NAME_TAG,
                                        0,
                                        entity.getViewYRot(partialTicks))
                        : null;
    }

    protected Component getNameTag(T entity) {
        return entity.getDisplayName();
    }

    /**
     * 26.x default submits the leash and name tag. backport: both are drawn natively by 1.21.1
     * (MobRenderer leash, EntityRenderer.render name tag), so the default submits nothing.
     */
    public void submit(
            S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {}

    protected boolean affectedByCulling(T entity) {
        return true;
    }

    protected AABB getBoundingBoxForCulling(T entity) {
        return entity.getBoundingBox();
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
        if (!entity.shouldRender(camX, camY, camZ)) return false;
        if (!affectedByCulling(entity)) return true;
        if (entity.noCulling) return true;
        AABB box = getBoundingBoxForCulling(entity).inflate(0.5);
        if (box.hasNaN() || box.getSize() == 0.0) {
            box = new AABB(
                    entity.getX() - 2.0,
                    entity.getY() - 2.0,
                    entity.getZ() - 2.0,
                    entity.getX() + 2.0,
                    entity.getY() + 2.0,
                    entity.getZ() + 2.0);
        }
        // backport: 1.21.1's leash-holder visibility check is left to super for leashable mobs
        return frustum.isVisible(box) || super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    /** 1.21.1 abstract; 26.x has no per-entity texture on the base renderer. */
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return MissingTextureAtlasSprite.getLocation();
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        S state = createRenderState();
        int saved = pendingLight;
        pendingLight = packedLight;
        try {
            extractRenderState(entity, state, partialTick);
        } finally {
            pendingLight = saved;
        }
        submit(state, poseStack, new ImmediateSubmitNodeCollector(buffers), CameraRenderState.current());
        // name tag (1.21.1 draws it here, after the model)
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }
}
