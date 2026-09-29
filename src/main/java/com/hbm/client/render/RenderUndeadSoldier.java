// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.core.VanillaMobRenderer;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

// backport: 1.21.1's ZombieModel only takes Zombie entities; the soldier uses a HumanoidModel on the
// zombie / skeleton layers with the zombie arm pose (what 26.x's ZombieModel does from the state),
// drawn by the vanilla renderer (VanillaMobRenderer). 26.x WingsLayer = 1.21.1 ElytraLayer.
public final class RenderUndeadSoldier
        extends VanillaMobRenderer<
                EntityUndeadSoldier, UndeadSoldierRenderState, HumanoidModel<EntityUndeadSoldier>>
        implements ConcurrentRenderStateExtraction {

    private static final ResourceLocation ZOMBIE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final ResourceLocation SKELETON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    private final HumanoidModel<EntityUndeadSoldier> zombieModel;
    private final HumanoidModel<EntityUndeadSoldier> skeletonModel;

    public RenderUndeadSoldier(EntityRendererProvider.Context context) {
        super(context, new SoldierModel(context.bakeLayer(ModelLayers.ZOMBIE)), .5F);
        zombieModel = model;
        skeletonModel = new SoldierModel(context.bakeLayer(ModelLayers.SKELETON));
        addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        addLayer(new ElytraLayer<>(this, context.getModelSet()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(
                new HumanoidArmorLayer<>(
                        this,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                        context.getModelManager()));
    }

    @Override
    public UndeadSoldierRenderState createRenderState() {
        return new UndeadSoldierRenderState();
    }

    @Override
    public void extractRenderState(
            EntityUndeadSoldier entity, UndeadSoldierRenderState state, float partialTicks) {
        state.soldierType = entity.soldierType();
    }

    @Override
    protected void beforeRender(UndeadSoldierRenderState state) {
        model =
                state.soldierType == EntityUndeadSoldier.TYPE_SKELETON
                        ? skeletonModel
                        : zombieModel;
    }

    @Override
    public ResourceLocation getTextureLocation(UndeadSoldierRenderState state) {
        return state.soldierType == EntityUndeadSoldier.TYPE_SKELETON
                ? SKELETON_TEXTURE
                : ZOMBIE_TEXTURE;
    }

    private static final class SoldierModel extends HumanoidModel<EntityUndeadSoldier> {
        SoldierModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(
                EntityUndeadSoldier entity,
                float limbSwing,
                float limbSwingAmount,
                float ageInTicks,
                float netHeadYaw,
                float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            AnimationUtils.animateZombieArms(
                    leftArm, rightArm, entity.isAggressive(), attackTime, ageInTicks);
        }
    }
}
