// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityRubble;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.blockmodel.BlockStateModelSet;
import com.hbm.backport.client.blockmodel.BlockStateModel;
import com.hbm.backport.client.blockmodel.BlockStateModelPart;
import com.hbm.backport.client.core.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.hbm.backport.client.core.EntityRenderState;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.hbm.backport.client.blockmodel.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class RenderRubble extends EntityRenderer<EntityRubble, RenderRubble.State>
        implements ConcurrentRenderStateExtraction {

    private static final Axis TUMBLE = Axis.of(new Vector3f(1F, 1F, 1F));

    private final ModelPart model = ModelRubble.createBodyLayer().bakeRoot();
    private final Map<BlockState, RenderType> skins = new ConcurrentHashMap<>();

    public RenderRubble(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityRubble entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.skin = skins.computeIfAbsent(entity.getBlockState(), RenderRubble::skinFor);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        poseStack.mulPose(TUMBLE.rotationDegrees(state.ageInTicks % 360F * 10F));
        collector.submitModelPart(
                model, poseStack, state.skin, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static RenderType skinFor(BlockState state) {
        ResourceLocation name = downSprite(state).contents().name();
        return RenderTypes.entityCutoutCull(
                ResourceLocation.fromNamespaceAndPath(
                        name.getNamespace(), "textures/" + name.getPath() + ".png"));
    }

    private static TextureAtlasSprite downSprite(BlockState state) {
        BlockStateModelSet models =
                com.hbm.backport.client.blockmodel.BlockStateModelSet.current();
        BlockStateModel model = models.get(state);
        List<BlockStateModelPart> parts = new ArrayList<>(1);
        model.collectParts(RandomSource.create(state.getSeed(BlockPos.ZERO)), parts);
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(Direction.DOWN);
            if (!quads.isEmpty()) return quads.getFirst().materialInfo().sprite();
        }
        return model.particleMaterial().sprite();
    }

    public static final class State extends EntityRenderState {
        RenderType skin;
    }
}
