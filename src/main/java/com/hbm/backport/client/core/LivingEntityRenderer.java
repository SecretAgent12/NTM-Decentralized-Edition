// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.entity.LivingEntityRenderer<T, S, M>} for mod mobs with
 * state-posed models ({@link EntityModel}). Ported from the 1.21.1 LivingEntityRenderer render path
 * onto render states. backport: 26.x render layers are not bridged (no mod mob with a 26.x model uses
 * them); vanilla-model mobs render through 1.21.1's own LivingEntityRenderer instead.
 */
public abstract class LivingEntityRenderer<
                T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends EntityRenderer<T, S> {
    protected M model;

    protected LivingEntityRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context);
        this.model = model;
        this.shadowRadius = shadowRadius;
    }

    public M getModel() {
        return model;
    }

    public abstract ResourceLocation getTextureLocation(S state);

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.hasPose(Pose.SLEEPING) && state.bedOrientation != null) {
            float eye = state.eyeHeight - 0.1F;
            poseStack.translate(
                    -state.bedOrientation.getStepX() * eye, 0.0F, -state.bedOrientation.getStepZ() * eye);
        }
        float scale = state.scale;
        poseStack.scale(scale, scale, scale);
        setupRotations(state, poseStack, state.bodyRot, scale);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        scale(state, poseStack);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        boolean bodyVisible = isBodyVisible(state);
        boolean translucent = !bodyVisible && !state.isInvisibleToPlayer;
        RenderType type = getRenderType(state, bodyVisible, translucent, state.appearsGlowing());
        if (type != null) {
            int overlay = getOverlayCoords(state, getWhiteOverlayProgress(state));
            int base = translucent ? 0x26FFFFFF : -1;
            int color = FastColor.ARGB32.multiply(base, getModelTint(state));
            collector.submitModel(
                    model, state, poseStack, type, state.lightCoords, overlay, color, null,
                    state.outlineColor, null);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    protected int getModelTint(S state) {
        return -1;
    }

    protected void scale(S state, PoseStack poseStack) {}

    protected @Nullable RenderType getRenderType(
            S state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        ResourceLocation texture = getTextureLocation(state);
        if (forceTransparent) return RenderType.itemEntityTranslucentCull(texture);
        if (isBodyVisible) return model.renderType(texture);
        return appearGlowing ? RenderType.outline(texture) : null;
    }

    public static int getOverlayCoords(LivingEntityRenderState state, float whiteOverlayProgress) {
        return OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(state.hasRedOverlay));
    }

    protected boolean isBodyVisible(S state) {
        return !state.isInvisible;
    }

    protected boolean isShaking(S state) {
        return state.isFullyFrozen;
    }

    protected void setupRotations(S state, PoseStack poseStack, float bodyRot, float entityScale) {
        if (isShaking(state)) {
            bodyRot += (float) (Math.cos(Mth.floor(state.ageInTicks) * 3.25F) * Math.PI * 0.4F);
        }
        if (!state.hasPose(Pose.SLEEPING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        }
        if (state.deathTime > 0.0F) {
            float f = (state.deathTime - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) f = 1.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * getFlipDegrees()));
        } else if (state.isAutoSpinAttack) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - state.xRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * -75.0F));
        } else if (state.hasPose(Pose.SLEEPING)) {
            Direction bed = state.bedOrientation;
            float angle = bed != null ? sleepDirectionToRotation(bed) : bodyRot;
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.mulPose(Axis.ZP.rotationDegrees(getFlipDegrees()));
            poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        } else if (state.isUpsideDown) {
            poseStack.translate(0.0F, (state.boundingBoxHeight + 0.1F) / entityScale, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    private static float sleepDirectionToRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 90.0F;
            case NORTH -> 270.0F;
            case EAST -> 180.0F;
            default -> 0.0F;
        };
    }

    protected float getFlipDegrees() {
        return 90.0F;
    }

    protected float getWhiteOverlayProgress(S state) {
        return 0.0F;
    }

    /** 1.21.1 LivingEntityRenderer.shouldShowName (team visibility rules). */
    @Override
    protected boolean shouldShowName(T entity) {
        double distSq = this.entityRenderDispatcher.distanceToSqr(entity);
        float limit = entity.isDiscrete() ? 32.0F : 64.0F;
        if (distSq >= (double) (limit * limit)) return false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean visible = player == null || !entity.isInvisibleTo(player);
        if (player != null && entity != player) {
            Team team = entity.getTeam();
            Team own = player.getTeam();
            if (team != null) {
                return switch (team.getNameTagVisibility()) {
                    case ALWAYS -> visible;
                    case NEVER -> false;
                    case HIDE_FOR_OTHER_TEAMS -> own == null
                            ? visible
                            : team.isAlliedTo(own) && (team.canSeeFriendlyInvisibles() || visible);
                    case HIDE_FOR_OWN_TEAM -> own == null ? visible : !team.isAlliedTo(own) && visible;
                };
            }
        }
        return Minecraft.renderNames()
                && entity != mc.getCameraEntity()
                && visible
                && !entity.isVehicle();
    }
}
