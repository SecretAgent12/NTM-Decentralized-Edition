// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.core.BoatRenderState;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.EntityRenderer;
import com.hbm.backport.client.core.Model;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.entity.item.EntityBoatRubber;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

// backport: 26.x AbstractBoatRenderer (state-based) does not exist in 1.21.1, whose BoatRenderer is
// tied to Boat.Type textures; its render path (1.21.1 BoatRenderer.render) is ported here onto the
// bridged 26.x-shaped EntityRenderer.
public class RenderBoatRubber extends EntityRenderer<EntityBoatRubber, BoatRenderState>
        implements ConcurrentRenderStateExtraction {

    private final Model.Simple waterPatchModel;
    private final ModelBoatRubber model;

    public RenderBoatRubber(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.waterPatchModel =
                // backport: 1.21.1 has no separate water patch layer; the patch is the
                // "water_patch" part of the vanilla boat model
                new Model.Simple(
                        context.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK))
                                .getChild("water_patch"),
                        ignored -> RenderType.waterMask());
        this.model = new ModelBoatRubber(ModelBoatRubber.createBodyLayer().bakeRoot());
    }

    @Override
    public BoatRenderState createRenderState() {
        return new BoatRenderState();
    }

    @Override
    public void extractRenderState(EntityBoatRubber boat, BoatRenderState state, float partialTicks) {
        super.extractRenderState(boat, state, partialTicks);
        state.yRot = boat.getViewYRot(partialTicks);
        state.hurtTime = boat.getHurtTime() - partialTicks;
        state.hurtDir = boat.getHurtDir();
        state.damageTime = Math.max(boat.getDamage() - partialTicks, 0.0F);
        state.bubbleAngle = boat.getBubbleAngle(partialTicks);
        state.isUnderWater = boat.isUnderWater();
        state.rowingTimeLeft = boat.getRowingTime(0, partialTicks);
        state.rowingTimeRight = boat.getRowingTime(1, partialTicks);
    }

    @Override
    public void submit(
            BoatRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        if (state.hurtTime > 0.0F) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10.0F
                                    * state.hurtDir));
        }
        if (!Mth.equal(state.bubbleAngle, 0.0F)) {
            poseStack.mulPose(
                    new Quaternionf()
                            .setAngleAxis(state.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        collector.submitModel(
                model,
                state,
                poseStack,
                model.renderType(ResourceManager.boat_rubber_tex),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null);
        if (!state.isUnderWater) {
            collector.submitModel(
                    waterPatchModel,
                    Unit.INSTANCE,
                    poseStack,
                    RenderType.waterMask(),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    state.outlineColor,
                    null);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
