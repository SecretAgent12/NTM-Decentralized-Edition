// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.core.HumanoidRenderState;
import com.hbm.backport.client.core.VanillaMobRenderer;
import com.hbm.entity.mob.EntityDummy;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import com.hbm.backport.client.rendertype.RenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

// backport: the 1.21.1 HumanoidModel is entity-posed, so the vanilla renderer draws it
// (VanillaMobRenderer); the layers are the ones 26.x's HumanoidMobRenderer adds (head, wings
// = 1.21.1 ElytraLayer, held items).
public class RenderHumanoidMob<T extends Mob>
        extends VanillaMobRenderer<T, HumanoidRenderState, HumanoidModel<T>>
        implements ConcurrentRenderStateExtraction {

    private final ResourceLocation texture;

    private RenderHumanoidMob(
            EntityRendererProvider.Context context,
            ResourceLocation texture,
            Function<ResourceLocation, RenderType> renderType) {
        super(
                context,
                new HumanoidModel<>(
                        LayerDefinition.create(
                                        HumanoidModel.createMesh(CubeDeformation.NONE, 0F), 64, 32)
                                .bakeRoot(),
                        renderType),
                0.5F);
        this.texture = texture;
        addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        addLayer(new ElytraLayer<>(this, context.getModelSet()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    public static RenderHumanoidMob<EntityGhost> ghost(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(
                context, ResourceManager.ghost_tex, RenderTypes::entityTranslucent);
    }

    public static RenderHumanoidMob<EntityDummy> dummy(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(
                context, ResourceManager.dummy_tex, RenderTypes::entityCutout);
    }

    public static RenderHumanoidMob<EntityFBI> fbi(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(context, ResourceManager.fbi_tex, RenderTypes::entityCutout);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(HumanoidRenderState state) {
        return texture;
    }
}
