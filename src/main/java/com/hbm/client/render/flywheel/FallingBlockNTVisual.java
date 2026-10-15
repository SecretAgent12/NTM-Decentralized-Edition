// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.item.EntityFallingBlockNT;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import com.hbm.backport.client.flywheel.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.BlockModelBuilder;
import com.hbm.backport.client.flywheel.AbstractEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.component.ShadowComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.hbm.backport.client.core.MovingBlockRenderState;
import com.hbm.backport.client.blockmodel.BlockStateModelPart;
import com.hbm.backport.client.blockmodel.BlockStateModelSet;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import com.hbm.backport.client.blockmodel.CardinalLighting;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FallingBlockNTVisual extends AbstractEntityVisual<EntityFallingBlockNT>
        implements SimpleDynamicVisual {
    private static final Map<Key, Model> MODELS = new ConcurrentHashMap<>();

    private final BlockState state;
    private final @Nullable TransformedInstance instance;
    private final @Nullable ShadowComponent shadow;
    private final Matrix4f pose = new Matrix4f();

    private record Key(
            BlockState state,
            List<BlockStateModelPart> parts,
            CardinalLighting cardinalLighting,
            boolean ambientOcclusion) {}

    public FallingBlockNTVisual(
            VisualizationContext context, EntityFallingBlockNT entity, float partialTick) {
        super(context, entity, partialTick);
        state = entity.getBlockState();
        if (!visualized(entity)) {
            instance = null;
            shadow = null;
            return;
        }
        ClientLevel client = (ClientLevel) level;
        Minecraft minecraft = Minecraft.getInstance();
        long seed = state.getSeed(entity.getStartPos());
        List<BlockStateModelPart> selected = new ArrayList<>(1);
        // backport: 26.x getModelManager().getBlockStateModelSet() / RandomSource
        // .createThreadLocalInstance -> DE's BlockStateModelSet shim / a seeded RandomSource
        BlockStateModelSet.current()
                .get(state)
                .collectParts(RandomSource.create(seed), selected);
        CardinalLighting cardinal = CardinalLighting.of(client);
        boolean ao = minecraft.options.ambientOcclusion().get();
        Key key = new Key(state, List.copyOf(selected), cardinal, ao);
        Model model =
                MODELS.computeIfAbsent(
                        key,
                        ignored -> {
                            MovingBlockRenderState snapshot = new MovingBlockRenderState();
                            BlockPos renderPos =
                                    BlockPos.containing(
                                            entity.getX(),
                                            entity.getBoundingBox().maxY,
                                            entity.getZ());
                            snapshot.blockState = state;
                            snapshot.blockPos = renderPos;
                            snapshot.randomSeedPos = entity.getStartPos();
                            snapshot.biome = client.getBiome(renderPos);
                            snapshot.cardinalLighting = cardinal;
                            snapshot.lightEngine = client.getLightEngine();
                            // backport: Flywheel 1.0.7 has no buildMovingBlock; its
                            // BlockModelBuilder tesselates the snapshot (a one-block level) at
                            // renderPos, so the pose moves the block back to the origin. Its
                            // variant random is seeded from renderPos, not the start position.
                            PoseStack origin = new PoseStack();
                            origin.translate(
                                    -renderPos.getX(), -renderPos.getY(), -renderPos.getZ());
                            return new BlockModelBuilder(snapshot, List.of(renderPos))
                                    .poseStack(origin)
                                    .build();
                        });
        instance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, model).createInstance();
        shadow = new ShadowComponent(context, entity);
        instance.setVisible(draw());
        animate(partialTick);
    }

    public static boolean visualized(EntityFallingBlockNT entity) {
        BlockState state = entity.getBlockState();
        return state.is(ModBlocks.SELLAFIELD_SLAKED.get())
                || state.is(ModBlocks.WASTE_LEAVES.get())
                || state.is(Blocks.DIRT)
                || state.is(Blocks.STONE);
    }

    public static void clearCache() {
        MODELS.clear();
    }

    private boolean draw() {
        return state.getRenderShape() == RenderShape.MODEL
                && state != level.getBlockState(entity.blockPosition());
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        if (instance == null || !isVisible(context.frustum())) return;
        boolean visible = draw();
        instance.setVisible(visible);
        if (!visible) {
            shadow.radius(0F);
            shadow.beginFrame(context);
            return;
        }
        animate(context.partialTick());
        shadow.radius(0.5F);
        // backport: 26.x Flywheel EntityFeatureCompat.shadowStrength is absent in Flywheel 1.0.7;
        // the 1.21.1 vanilla shadow weight (1 - d^2 / 256) * shadowStrength (1) is used, floored
        // at 0 (vanilla skips the shadow there)
        shadow.strength(
                Math.max(
                        0F,
                        (float) (1.0 - entity.distanceToSqr(context.camera().getPosition()) / 256.0)));
        shadow.beginFrame(context);
    }

    private void animate(float partialTick) {
        var origin = renderOrigin();
        float x = (float) (Mth.lerp(partialTick, entity.xOld, entity.getX()) - origin.getX());
        float y = (float) (Mth.lerp(partialTick, entity.yOld, entity.getY()) - origin.getY());
        float z = (float) (Mth.lerp(partialTick, entity.zOld, entity.getZ()) - origin.getZ());
        pose.translation(x, y, z).translate(-0.5F, 0F, -0.5F);
        instance.setTransform(pose).light(computePackedLight(partialTick)).setChanged();
    }

    @Override
    protected void _delete() {
        if (instance != null) instance.delete();
        if (shadow != null) shadow.delete();
    }
}
